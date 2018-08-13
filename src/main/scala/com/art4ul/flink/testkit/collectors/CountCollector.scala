package com.art4ul.flink.testkit.collectors

import com.art4ul.flink.testkit.matchers.ResultMatcher

import scala.collection.mutable.ListBuffer

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