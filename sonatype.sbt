import xerial.sbt.Sonatype._

sonatypeProfileName := "com.art4ul"

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://art4ul.com"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/art4ul/flink-testkit"),
    "scm:git@github.com:art4ul/flink-testkit.git"
  )
)
developers := List(
  Developer(id="art4ul", name="Artsem Semianenka", email="artfulonline@gmail.com", url=url("http://art4ul.com"))
)