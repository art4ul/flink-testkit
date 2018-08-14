# Flink-testkit
Simple framework for testing Flink streaming applications based on ScalaTest library.

## Example 
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

## How to use
### SBT 
```scala
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  
  libraryDependencies += "com.art4ul" %% "flink-testkit" % "0.1-SNAPSHOT"
```

### Maven 
Add maven repository :
```xml
<repositories>
    <repository>
        <id>oss-sonatype</id>
        <name>oss-sonatype</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```
and dependency to pom.xml:
```xml
<dependency>
    <groupId>com.art4ul</groupId>
    <artifactId>flink-testkit</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

