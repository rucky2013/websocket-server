package com.company.util

/**
  * Created by bl05959 on 2016/7/29.
  */
trait CommonProperties {
  def kafkaBrokers: String

  def kafkaOffsetManagerZkServers: String

  def kafkaOffsetManagerZkPort: Int

  def offsetZkNodePath: String

}
