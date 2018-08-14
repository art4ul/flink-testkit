# Flink-testkit
Simple framework for testing Flink streaming applications based on ScalaTest library.

# Example 
Create test source:
```scala
class ExampleSpec extends FlatSpec with FlinkTestBase {

  "TestSource" should "emit messages" in {
    import com.art4ul.flink.testkit.source._
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val source = env.testSource[String]()
      .emit("msg1")
      .emit("msg2", delay = 2 second)
      .watermark(-1 seconds)
      .emit("msg3", delay = -1 second)
      .emit("msg4", delay = 1 second)
      .watermark(-1 seconds)
      .create

    import com.art4ul.flink.testkit.sink._
    val expected = Set(
      "msg1",
      "msg2",
      "msg3",
      "msg4"
    )
    source.unboundedTestSink()
      .collect(expectCount = 4, SetEqualityMatcher(expected))
      .create

    env.execute()
  }
}
```

