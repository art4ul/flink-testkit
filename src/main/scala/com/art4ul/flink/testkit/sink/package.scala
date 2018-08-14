package com.art4ul.flink.testkit

import com.art4ul.flink.testkit.runtime.TestExecutionEnvironment
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

package object sink {

  implicit class RichFlow[T](flow: DataStream[T]) {

    def unboundedTestSink(timeout: FiniteDuration = 10.seconds): UnboundedTestSinkBuilder[T] = {
      val env: TestExecutionEnvironment = flow.executionEnvironment match {
        case testEnv: TestExecutionEnvironment => testEnv

        case scalaWrapper: StreamExecutionEnvironment =>
          require(scalaWrapper.getJavaEnv.isInstanceOf[TestExecutionEnvironment], "Incorrect execution environment")
          scalaWrapper.getJavaEnv.asInstanceOf[TestExecutionEnvironment]

        case _ => throw new IllegalArgumentException("Incorrect execution environment")
      }

      UnboundedTestSinkBuilder(env, flow, timeout)
    }
  }
}
