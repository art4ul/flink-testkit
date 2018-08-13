package com.art4ul.flink.testkit.matchers

class SetEqualityMatcher[T](expectedSet: Set[T]) extends ResultMatcher[T] {

  def matchResult(buffer: Seq[T]): Unit = {
    val result = buffer.toSet
    assert(result == expectedSet, s"Sets are not equal [Actual:$result] [Expected:$expectedSet]")
  }

}

object SetEqualityMatcher{
  def apply[T](expectedSet: Set[T]): SetEqualityMatcher[T] = new SetEqualityMatcher[T](expectedSet)
}