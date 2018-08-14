package com.art4ul.flink.testkit

import com.art4ul.flink.testkit.runtime.ScalaTestExecutionEnvironment
import org.scalatest.{BeforeAndAfter, Suite}

trait FlinkTestBase extends BeforeAndAfter {
  this: Suite =>

  var env: ScalaTestExecutionEnvironment = _

  before {
    env = ScalaTestExecutionEnvironment()
  }

  after {
    env.close
  }

}
