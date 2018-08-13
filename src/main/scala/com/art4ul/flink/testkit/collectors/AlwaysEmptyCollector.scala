package com.art4ul.flink.testkit.collectors

class AlwaysEmptyCollector[T] extends ResultCollector[T] {

  override def ready: Boolean = true

  override def add(elem: T): Unit = throw new java.lang.AssertionError("Sink should not receive any message")

  override def matchingTrigger: Unit = {}
}
