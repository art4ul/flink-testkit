package com.art4ul.flink.testkit

object ResultMatchers {

  trait ResultMatcher[T] extends Serializable {

    def matchResult(buffer: Seq[T]): Unit
  }

  class SetEqualityMatcher[T](expectedSet: Set[T]) extends ResultMatcher[T] {

    def matchResult(buffer: Seq[T]): Unit = {
      val result = buffer.toSet
      assert(result == expectedSet, s"Sets are not equal [Actual:$result] [Expected:$expectedSet]")
    }

  }

  object SetEqualityMatcher{
    def apply[T](expectedSet: Set[T]): SetEqualityMatcher[T] = new SetEqualityMatcher[T](expectedSet)
  }

  class SumMatcher(expectedSum: Int) extends ResultMatcher[Long] {

    def matchResult(buffer: Seq[Long]): Unit = {

      val result = buffer.fold(0L)(_ + _)
      assert(result == expectedSum, s"Sum of all elements should be [$expectedSum]")
    }

  }

  object SumMatcher{
    def apply(expectedSum: Int): SumMatcher = new SumMatcher(expectedSum)
  }

}