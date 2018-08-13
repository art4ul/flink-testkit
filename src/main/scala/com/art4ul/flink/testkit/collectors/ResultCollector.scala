package com.art4ul.flink.testkit.collectors

trait ResultCollector[T] extends Serializable {

  def ready: Boolean

  def add(elem: T): Unit

  def matchingTrigger: Unit
}
