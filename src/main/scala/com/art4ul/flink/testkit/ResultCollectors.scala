package com.art4ul.flink.testkit

import com.art4ul.flink.testkit.ResultMatchers.ResultMatcher

import scala.collection.mutable.ListBuffer

object ResultCollectors {

  trait ResultCollector[T] extends Serializable {

    def ready: Boolean

    def add(elem: T): Unit

    def matchingTrigger: Unit
  }

  class CountCollector[T](count: Int, matcher: ResultMatcher[T]) extends ResultCollector[T] {

    private val buffer = new ListBuffer[T]()

    def ready: Boolean = buffer.size == count

    def add(elem: T): Unit = {
      buffer += elem
    }

    def matchingTrigger: Unit = {
      matcher.matchResult(buffer)
    }

  }

  class AlwaysEmptyCollector[T] extends ResultCollector[T] {

    override def ready: Boolean = true

    override def add(elem: T): Unit = throw new java.lang.AssertionError("Sink should not receive any message")

    override def matchingTrigger: Unit = {}
  }

}