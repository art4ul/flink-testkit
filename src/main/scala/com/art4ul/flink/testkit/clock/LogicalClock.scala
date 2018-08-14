package com.art4ul.flink.testkit.clock
import scala.concurrent.duration.FiniteDuration

class LogicalClock(init:Long) extends Clock{

  private var time:Long  = init

  override def getTime: Long = time

  override def tick(increment: FiniteDuration): Long = {
    time += increment.toMillis
    time
  }
}
