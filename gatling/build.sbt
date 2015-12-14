import sbt._
import io.gatling.sbt.GatlingPlugin

name := "rprng.gatling"
organization := "com.jejking"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val gatlingV = "2.1.7"
  Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts"         % gatlingV    % "test",
    "io.gatling"            % "gatling-test-framework"            % gatlingV    % "test"
  )
}

enablePlugins(GatlingPlugin)
