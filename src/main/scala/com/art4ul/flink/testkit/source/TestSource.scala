package com.art4ul.flink.testkit.source

import com.art4ul.flink.testkit.clock.{Clock, LogicalClock, SystemClock}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.watermark.Watermark

import scala.concurrent.duration.FiniteDuration

trait Element

case class WatermarkElement(timestamp: Long) extends Element

case class Msg[T](value: T, timestamp: Option[Long]) extends Element

class TestSource[OUT](elements: Seq[Element]) extends RichSourceFunction[OUT] {

  override def run(ctx: SourceFunction.SourceContext[OUT]): Unit = {
    elements.foreach {
      case WatermarkElement(time) => ctx.emitWatermark(new Watermark(time))
      case Msg(value: OUT, None) => ctx.collect(value)
      case Msg(value: OUT, Some(time)) => ctx.collectWithTimestamp(value, time)
    }
  }

  override def cancel(): Unit = {}
}

protected[source] case class TestSourceBuilder[T: TypeInformation](private val env: StreamExecutionEnvironment,
                                                         private val clock: Clock,
                                                         private val elements: Seq[Element] = Seq()) {

  def ingestionTime: TestSourceBuilder[T] = this.copy(clock = new SystemClock)

  def emit(msg: T): TestSourceBuilder[T] = {
    this.copy(elements = elements :+ Msg(msg, None))
  }

  def emit(msg: T, delay: FiniteDuration): TestSourceBuilder[T] = {
    val time = clock.tick(delay)
    this.copy(elements = elements :+ Msg(msg, Some(time)))
  }

  def emit(msg: T, time: Long): TestSourceBuilder[T] = {
    this.copy(elements = elements :+ Msg(msg, Some(time)))
  }

  def watermark(increment: FiniteDuration): TestSourceBuilder[T] = {
    val time = clock.tick(increment)
    this.copy(elements = elements :+ WatermarkElement(time))
  }

  def watermark(time: Long): TestSourceBuilder[T] = {
    this.copy(elements = elements :+ WatermarkElement(time))
  }

  def create(): DataStream[T] = {
    env.addSource(new TestSource[T](elements))
      .name("Test Source")
  }

}
