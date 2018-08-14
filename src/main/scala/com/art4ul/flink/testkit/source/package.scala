package com.art4ul.flink.testkit

import com.art4ul.flink.testkit.clock.LogicalClock
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

package object source {

  implicit class RichStreamExecutionEnvironment(env: StreamExecutionEnvironment) {
    def testSource[T: TypeInformation](initTime: Long = 0): TestSourceBuilder[T] = {
      TestSourceBuilder(env, new LogicalClock(initTime))
    }
  }

}
