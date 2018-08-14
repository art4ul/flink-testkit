package com.art4ul.flink.testkit.clock
import scala.concurrent.duration.FiniteDuration

class SystemClock extends Clock {
  override def getTime: Long = System.currentTimeMillis()

  override def tick(increment: FiniteDuration): Long = {
    Thread.sleep(increment.toMillis)
    System.currentTimeMillis()
  }
}
