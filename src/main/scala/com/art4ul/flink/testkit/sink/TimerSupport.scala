package com.art4ul.flink.testkit.sink

import akka.actor.{ActorSystem, Cancellable}

import scala.concurrent.duration.FiniteDuration

trait TimerSupport {

  @transient private lazy val actorSystem = ActorSystem()

  @transient private var timer: Cancellable = _

  def onTimer(): Unit

  protected def setTimer(timeout: FiniteDuration): Unit = {
    implicit val dispatcher = actorSystem.dispatcher
    this.synchronized {
      timer = actorSystem.scheduler.scheduleOnce(timeout) {
        onTimer()
      }
    }
  }

  protected def cancelTimer(): Unit = {
    this.synchronized {
      if (timer != null) {
        timer.cancel()
      }
    }
  }

}
