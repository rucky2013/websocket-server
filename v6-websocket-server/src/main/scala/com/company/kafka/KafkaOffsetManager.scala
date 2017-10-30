package com.company.kafka

import kafka.common.TopicAndPartition

/**
  * Created by bl05959 on 2016/9/12.
  */
trait KafkaOffsetManager {
  def storeOffset(topic: String, offsets: java.util.Map[Int, Long])

  def getOffset(topic: String, readFrom: String): java.util.Map[TopicAndPartition, Long]
}
