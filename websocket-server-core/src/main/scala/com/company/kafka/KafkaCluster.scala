package com.company.kafka

import kafka.api._
import kafka.common.{ErrorMapping, TopicAndPartition}
import kafka.consumer.SimpleConsumer

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

/**
  * Created by bg244210 on 2016/9/12.
  * Same as org.apache.spark.streaming.kafka.KafkaCluster
  */
class KafkaCluster(brokers: String) {

  import KafkaCluster._

  private val brokerAddress = brokers.split(",").map(_.split(":")).map(arr => (arr(0), arr(1).toInt))

  def getEarliestLeaderOffsets(topicAndPartitions: Set[TopicAndPartition]): Either[Err, Map[TopicAndPartition, Long]] =
    getLeaderOffsets(topicAndPartitions, OffsetRequest.EarliestTime)

  def getLatestLeaderOffsets(topicAndPartitions: Set[TopicAndPartition]): Either[Err, Map[TopicAndPartition, Long]] =
    getLeaderOffsets(topicAndPartitions, OffsetRequest.LatestTime)

  def getLeaderOffsets(topicAndPartitions: Set[TopicAndPartition], before: Long): Either[Err, Map[TopicAndPartition, Long]] = {
    getLeaderOffsets(topicAndPartitions, before, 1).right.map { r =>
      r.map { kv =>
        // mapValues isnt serializable, see SI-7005
        kv._1 -> kv._2.head
      }
    }
  }

  private def flip[K, V](m: Map[K, V]): Map[V, Seq[K]] = {
    m.groupBy(_._2).map { kv =>
      kv._1 -> kv._2.keys.toSeq
    }
  }

  def getLeaderOffsets(
                        topicAndPartitions: Set[TopicAndPartition],
                        before: Long,
                        maxNumOffsets: Int
                      ): Either[Err, Map[TopicAndPartition, Seq[Long]]] = {
    findLeaders(topicAndPartitions).right.flatMap { tpToLeader =>
      val leaderToTp: Map[(String, Int), Seq[TopicAndPartition]] = flip(tpToLeader)
      val leaders = leaderToTp.keys
      var result = Map[TopicAndPartition, Seq[Long]]()
      val errs = new Err
      withBrokers(leaders, errs) { consumer =>
        val partitionsToGetOffsets: Seq[TopicAndPartition] = leaderToTp((consumer.host, consumer.port))
        val reqMap = partitionsToGetOffsets.map { tp: TopicAndPartition =>
          tp -> PartitionOffsetRequestInfo(before, maxNumOffsets)
        }.toMap
        val req = OffsetRequest(reqMap)
        val resp = consumer.getOffsetsBefore(req)
        val respMap = resp.partitionErrorAndOffsets
        partitionsToGetOffsets.foreach { tp: TopicAndPartition =>
          respMap.get(tp).foreach { por: PartitionOffsetsResponse =>
            if (por.error == ErrorMapping.NoError) {
              if (por.offsets.nonEmpty) {
                result += tp -> por.offsets
              } else {
                errs.append(new IllegalArgumentException(
                  s"Empty offsets for $tp, is $before before log beginning?"))
              }
            } else {
              errs.append(ErrorMapping.exceptionFor(por.error))
            }
          }
        }
        if (result.keys.size == topicAndPartitions.size) {
          return Right(result)
        }
      }
      val missing = topicAndPartitions.diff(result.keySet)
      errs.append(new IllegalArgumentException(s"Couldn't find leader offsets for $missing"))
      Left(errs)
    }
  }

  def findLeaders(topicAndPartitions: Set[TopicAndPartition]): Either[Err, Map[TopicAndPartition, (String, Int)]] = {
    val topics = topicAndPartitions.map(_.topic)
    val response = getPartitionMetadata(topics).right
    val answer = response.flatMap { tms: Set[TopicMetadata] =>
      val leaderMap = tms.flatMap { tm: TopicMetadata =>
        tm.partitionsMetadata.flatMap { pm: PartitionMetadata =>
          val tp = TopicAndPartition(tm.topic, pm.partitionId)
          if (topicAndPartitions(tp)) {
            pm.leader.map { l =>
              tp -> (l.host -> l.port)
            }
          } else {
            None
          }
        }
      }.toMap

      if (leaderMap.keys.size == topicAndPartitions.size) {
        Right(leaderMap)
      } else {
        val missing = topicAndPartitions.diff(leaderMap.keySet)
        val err = new Err
        err.append(new IllegalArgumentException(s"Couldn't find leaders for $missing"))
        Left(err)
      }
    }
    answer
  }

  def getPartitionMetadata(topics: Set[String]): Either[Err, Set[TopicMetadata]] = {
    val req = new TopicMetadataRequest(topics.toSeq, 0)

    val errs = new Err
    withBrokers(brokerAddress, errs) { consumer =>
      val resp: TopicMetadataResponse = consumer.send(req)
      val respErrs = resp.topicsMetadata.filter(m => m.errorCode != ErrorMapping.NoError)

      if (respErrs.isEmpty) {
        return Right(resp.topicsMetadata.toSet)
      } else {
        respErrs.foreach { m =>
          val cause = ErrorMapping.exceptionFor(m.errorCode)
          val msg = s"Error getting partition metadata for '${m.topic}'. Does the topic exist?"
          throw new IllegalArgumentException(msg, cause)
        }
      }
    }
    Left(errs)
  }

  def getPartitions(topics: Set[String]): Either[Err, Set[TopicAndPartition]] = {
    getPartitionMetadata(topics).right.map { r =>
      r.flatMap { tm: TopicMetadata =>
        tm.partitionsMetadata.map { pm: PartitionMetadata =>
          TopicAndPartition(tm.topic, pm.partitionId)
        }
      }
    }
  }

  def connectLeader(topic: String, partition: Int): Either[Err, SimpleConsumer] =
    findLeader(topic, partition).right.map(hp => connect(hp._1, hp._2))


  def findLeader(topic: String, partition: Int): Either[Err, (String, Int)] = {
    val req = new TopicMetadataRequest(Seq(topic), 0)
    val errs = new Err
    withBrokers(brokerAddress, errs) { consumer =>
      val resp: TopicMetadataResponse = consumer.send(req)
      resp.topicsMetadata.find(_.topic == topic).flatMap { tm: TopicMetadata =>
        tm.partitionsMetadata.find(_.partitionId == partition)
      }.foreach { pm: PartitionMetadata =>
        pm.leader.foreach { leader =>
          return Right((leader.host, leader.port))
        }
      }
    }
    Left(errs)
  }

  def connect(host: String, port: Int): SimpleConsumer =
    new SimpleConsumer(host, port, 100000,
      64 * 1024, "metadataGetter")

  // Try a call against potentially multiple brokers, accumulating errors
  private def withBrokers(brokers: Iterable[(String, Int)], errs: Err)(fn: SimpleConsumer => Any): Unit = {
    brokers.foreach { hp =>
      var consumer: SimpleConsumer = null
      try {
        consumer = connect(hp._1, hp._2)
        fn(consumer)
      } catch {
        case NonFatal(e) =>
          errs.append(e)
      } finally {
        if (consumer != null) {
          consumer.close()
        }
      }
    }
  }
}

object KafkaCluster {
  type Err = ArrayBuffer[Throwable]

  /** If the result is right, return it, otherwise throw WyvernTrException */
  def checkErrors[T](result: Either[Err, T]): T = {
    result.fold(
      errs => throw new IllegalArgumentException(errs.mkString("\n")),
      ok => ok
    )
  }
}
