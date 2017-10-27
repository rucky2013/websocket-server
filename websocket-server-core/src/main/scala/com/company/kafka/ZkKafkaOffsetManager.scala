package com.company.kafka


import com.company.kafka.KafkaCluster.Err
import com.company.util.{Environment}
import com.company.zookeeper.ZkCliHelper
import kafka.common.TopicAndPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Created by bl05959 on 2016/9/12.
  * 管理kafka的offset
  */
class ZkKafkaOffsetManager(appName: String) extends KafkaOffsetManager {
  val logger = LoggerFactory.getLogger(getClass)

  private val ZkCliHelper = new ZkCliHelper(Environment.kafkaOffsetManagerZkServers, Environment.kafkaOffsetManagerZkPort)
  private val rootDir = s"${Environment.offsetZkNodePath}/$appName/"
  private val kafkaCluster = new KafkaCluster(Environment.kafkaBrokers)

  override def storeOffset(topic: String, offsets: java.util.Map[Int, Long]): Unit = {

    ZkCliHelper.writeJson(rootDir + topic, offsets.asScala.toMap.map(kv => kv._1.toString -> kv._2.toString))
    logger.info(s"topic $topic has store $offsets to zookeeper")
  }

  private def errDeal[T](either: Either[Err, T]): T = {
    either match {
      case Left(err) =>
        err.foreach(_.printStackTrace())
        throw new IllegalArgumentException("kafka err")
      case Right(x) => x
    }
  }

  override def getOffset(topic: String, readFrom: String): java.util.Map[TopicAndPartition, Long] = {
    val partitions = errDeal(kafkaCluster.getPartitions(Set(topic)))

    val zkOffsets = ZkCliHelper.readJson(rootDir + topic).map(offsets => {
      offsets.map { case (partition, offset) => (TopicAndPartition(topic, partition.toInt), offset.toLong) }
    }).getOrElse(Map())
    val earliestOffsets = errDeal(kafkaCluster.getEarliestLeaderOffsets(partitions))

    if (zkOffsets.isEmpty || zkOffsets.keySet != partitions) {
      logger.warn(s"no previous offset found or kafka cluster changed, read at $readFrom," +
        s" zkOffsets: $zkOffsets, kafka partitions: $partitions")
      if (readFrom == "smallest") {
        earliestOffsets.asJava
      } else {
        errDeal(kafkaCluster.getLatestLeaderOffsets(partitions)).asJava
      }
    } else {
      zkOffsets.map { case (topicAndPartition, offset) =>
        val earliestOffset = earliestOffsets(topicAndPartition)
        if (earliestOffset > offset) {
          logger.error("has lose some data!")
          (topicAndPartition, earliestOffset)
        } else {
          (topicAndPartition, offset)
        }
      }.asJava
    }
  }
}
