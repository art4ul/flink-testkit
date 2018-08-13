package com.art4ul.flink.testkit

import java.util.concurrent.ConcurrentHashMap

import akka.actor.{ActorSystem, Cancellable}
import com.art4ul.flink.testkit.ResultCollectors.{AlwaysEmptyCollector, CountCollector, ResultCollector}
import com.art4ul.flink.testkit.ResultMatchers.ResultMatcher
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise, TimeoutException}


object ResultBuffer {
  val sinkStates = new ConcurrentHashMap[String, Future[Boolean]]
}

object TestSink {

  implicit class RichFlow[T](flow: DataStream[T]) {

    def infinityTestSink(timeout: FiniteDuration = 10.seconds): TestSinkBuilder[T] = {
      val env: TestExecutionEnvironment = flow.executionEnvironment match {
        case testEnv: TestExecutionEnvironment => testEnv
        case scalaWrapper: StreamExecutionEnvironment =>
          require(scalaWrapper.getJavaEnv.isInstanceOf[TestExecutionEnvironment], "Incorrect execution environment")
          scalaWrapper.getJavaEnv.asInstanceOf[TestExecutionEnvironment]
        case _ => throw new IllegalArgumentException("Incorrect execution environment")
      }
      TestSinkBuilder(env, flow, timeout)
    }
  }

}

import java.util.UUID.randomUUID

case class TestSinkBuilder[T](env: TestExecutionEnvironment,
                              flow: DataStream[T],
                              timeout: FiniteDuration,
                              private val collectors: Seq[ResultCollector[T]] = Seq()) {

  def withCollector(collector: ResultCollector[T]): TestSinkBuilder[T] = {
    this.copy(collectors = collectors :+ collector)
  }

  def shouldNotReceiveAnyMessage: TestSinkBuilder[T] = {
    this.copy(collectors = collectors :+ new AlwaysEmptyCollector[T])
  }

  def collect(count: Int, matcher: ResultMatcher[T]): TestSinkBuilder[T] = {
    this.copy(collectors = collectors :+ new CountCollector(count, matcher))
  }

  def create: Unit = {
    val uuid = randomUUID.toString
    ResultBuffer.sinkStates.put(uuid, Promise.apply[Boolean]().future)
    flow.addSink(new TestSink[T](uuid, collectors, timeout)).setParallelism(1)
  }

}

trait TimerSupport {

  @transient private lazy val actorSystem = ActorSystem()

  @transient private var timer: Cancellable = _

  def onTimer(): Unit

  protected def setTimer(timeout: FiniteDuration): Unit = {
    implicit val dispatcher = actorSystem.dispatcher
    timer = actorSystem.scheduler.scheduleOnce(timeout) {
      onTimer()
    }
  }

  protected def cancelTimer(): Unit = {
    if (timer != null) {
      timer.cancel()
    }
  }

}

private class TestSink[T](name: String, collectors: Seq[ResultCollector[T]], timeout: FiniteDuration)
  extends RichSinkFunction[T] with TimerSupport {

  @transient
  @volatile private lazy val promise: Promise[Boolean] = Promise[Boolean]()

  @transient private lazy val collectorBuffer = mutable.Queue[ResultCollector[T]](collectors: _*)

  override def open(parameters: Configuration): Unit = {
    ResultBuffer.sinkStates.put(name, promise.future)
  }

  override def close(): Unit = {
    super.close()
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    try {
      cancelTimer()
      if (collectorBuffer.nonEmpty) {
        val collector = collectorBuffer.head
        collector.add(value)
        if (collector.ready) {
          collector.matchingTrigger
          collectorBuffer.dequeue()
        }
      }

      if (collectorBuffer.isEmpty) {
        promise.success(true)
      }
      setTimer(timeout)
    } catch {
      case ex: Throwable =>
        promise.failure(ex)
        throw ex
    }
  }

  override def onTimer(): Unit = {
    val ex = new TimeoutException("Messages was not received by sink during timeout")
    promise.failure(ex)
    throw ex
  }
}