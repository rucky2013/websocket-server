package com.company.zookeeper

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.zookeeper.CreateMode
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
/**
  * Created by bl05959 on 2016/9/12.
  * 管理 zookeeper 连接
  */
class ZkCliHelper(servers: String, port: Int) {
  val logger = LoggerFactory.getLogger(getClass)

  private val curator = newCurator(servers, port)
  curator.start()

  private def newCurator(
                  servers: String,
                  port: Int,
                  sessionTimeoutMs: Int = 20000,
                  connectionTimeoutMs: Int = 15000,
                  zkRetryTimes: Int = 5,
                  zkRetryIntervalMs: Int = 1000
                ): CuratorFramework = {
    val serverPorts = servers.split(",").map(_ + ":" + port).mkString(",")
    CuratorFrameworkFactory.newClient(
      serverPorts,
      sessionTimeoutMs,
      connectionTimeoutMs,
      new RetryNTimes(zkRetryTimes, zkRetryIntervalMs))
  }

  def writeJson(path: String, data: Map[String, String]): Unit = {
    val jsonData = JSON.toJSONString(data.asJava, SerializerFeature.QuoteFieldNames)
    logger.info("Writing " + path + " the data " + jsonData)
    writeBytes(path, jsonData.getBytes("UTF-8"))
  }

  def writeBytes(path: String, bytes: Array[Byte]): Unit = {
    if (curator.checkExists().forPath(path) == null) {
      curator.create()
        .creatingParentsIfNeeded()
        .withMode(CreateMode.PERSISTENT)
        .forPath(path, bytes)
    } else {
      curator.setData().forPath(path, bytes)
    }
  }

  def readJson(path: String): Option[Map[String, String]] = {
      readBytes(path).map(b =>JSON.parseObject(new String(b, "UTF-8"), classOf[java.util.Map[String, String]]).asScala.toMap)
  }

  def readBytes(path: String): Option[Array[Byte]] = {
      if (curator.checkExists.forPath(path) != null)
        Some(curator.getData.forPath(path))
      else None
  }

  def close(): Unit = {
    curator.close()
  }
}
