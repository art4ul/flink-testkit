package com.art4ul.flink.testkit.matchers

class SumMatcher(expectedSum: Int) extends ResultMatcher[Long] {

  def matchResult(buffer: Seq[Long]): Unit = {

    val result = buffer.fold(0L)(_ + _)
    assert(result == expectedSum, s"Sum of all elements should be [$expectedSum]")
  }

}

object SumMatcher {
  def apply(expectedSum: Int): SumMatcher = new SumMatcher(expectedSum)
}

