package com.art4ul.flink.testkit.clock

import scala.concurrent.duration.FiniteDuration

trait Clock extends Serializable {
  def getTime: Long

  def tick(increment: FiniteDuration): Long
}