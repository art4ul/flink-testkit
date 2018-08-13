package com.art4ul.flink.testkit

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
