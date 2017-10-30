package com.company.util

import java.util.Properties

import scala.collection.JavaConverters._

/**
  * Created by bl05959 on 2016/7/29.
  * 由environment.properties配置的环境变量
  */
object Environment extends CommonProperties {
  val env = {
    val props = new Properties()
    props.load(getClass.getClassLoader.getResourceAsStream("environment.properties"))
    props.asScala.toMap
  }


  override val kafkaBrokers: String = env("kafka.brokers")

  override def kafkaOffsetManagerZkServers: String = env("kafka.offset.manager.zookeeper.server")

  override def kafkaOffsetManagerZkPort: Int = env("kafka.offset.manager.zookeeper.port").toInt

  override def offsetZkNodePath: String = env("consumer.offset.rootDir")

}
