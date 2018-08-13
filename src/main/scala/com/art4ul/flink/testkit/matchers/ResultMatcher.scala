package com.art4ul.flink.testkit.matchers


trait ResultMatcher[T] extends Serializable {

  def matchResult(buffer: Seq[T]): Unit
}



