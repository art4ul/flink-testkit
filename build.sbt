name := "flink-testkit"

organization := "com.art4ul"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies ++=Seq(
  "org.apache.flink" %% "flink-streaming-scala" % "1.6.0" ,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.scalatest" %% "scalatest" % "3.0.5"
)

parallelExecution in ThisBuild := false