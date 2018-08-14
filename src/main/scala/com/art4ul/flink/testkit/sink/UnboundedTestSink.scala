package com.art4ul.flink.testkit.sink

import java.util.concurrent.ConcurrentHashMap

import com.art4ul.flink.testkit.collectors.{AlwaysEmptyCollector, CountCollector, ResultCollector}
import com.art4ul.flink.testkit.runtime.TestExecutionEnvironment
import com.art4ul.flink.testkit.matchers.ResultMatcher
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise, TimeoutException}


object ResultBuffer {
  val sinkStates = new ConcurrentHashMap[String, Future[Boolean]]
}

import java.util.UUID.randomUUID

protected[sink] case class UnboundedTestSinkBuilder[T](private val env: TestExecutionEnvironment,
                                                       private val flow: DataStream[T],
                                                       private val timeout: FiniteDuration,
                                                       private val collectors: Seq[ResultCollector[T]] = Seq()) {

  def withCollector(collector: ResultCollector[T]): UnboundedTestSinkBuilder[T] = {
    this.copy(collectors = collectors :+ collector)
  }

  def shouldNotReceiveAnyMessage: UnboundedTestSinkBuilder[T] = {
    this.copy(collectors = collectors :+ new AlwaysEmptyCollector[T])
  }

  def collect(expectCount: Int, matcher: ResultMatcher[T]): UnboundedTestSinkBuilder[T] = {
    this.copy(collectors = collectors :+ new CountCollector(expectCount, matcher))
  }

  def create(): DataStreamSink[T] = {
    val uuid = randomUUID.toString
    ResultBuffer.sinkStates.put(uuid, Promise.apply[Boolean]().future)
    flow.addSink(new UnboundedTestSink[T](uuid, collectors, timeout))
      .setParallelism(1)
      .name("Unbounded Test Sink")
  }

}

private class UnboundedTestSink[T](name: String, collectors: Seq[ResultCollector[T]], timeout: FiniteDuration)
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
      } else {
        setTimer(timeout)
      }
    } catch {
      case ex: Throwable =>
        cancelTimer()
        promise.failure(ex)
        throw ex
    }
  }

  override def onTimer(): Unit = {
    val ex = new TimeoutException("Messages was not received by sink during timeout")
    if (!promise.isCompleted) {
      promise.failure(ex)
    }
    throw ex
  }
}