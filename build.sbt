name := "Renderer"

version := "0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.7" % "test->default",
  "org.scalafx" %% "scalafx" % "1.0.0-M6",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3"
)

unmanagedJars in Compile += Attributed.blank(
    file(scala.util.Properties.javaHome) / "lib" / "jfxrt.jar")

fork in run := true

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

