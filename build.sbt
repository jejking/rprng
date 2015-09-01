import sbt._

name := "rprng"
organization := "com.jejking"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.7"


scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.3.12"
  val akkaStreamV = "1.0"
  val scalaTestV  = "2.2.5"
  val commonsMathV = "3.5"
  val scalaMockV = "3.2.2"
  Seq(
    "org.apache.commons" % "commons-math3"                        % commonsMathV,
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV % "test",
    "org.scalatest"     %% "scalatest"                            % scalaTestV  % "test",
    "com.typesafe.akka" %% "akka-testkit"                         % akkaV       % "test",
    "org.scalamock"     %% "scalamock-core"                       % scalaMockV     % "test",
    "org.scalamock"     %% "scalamock-scalatest-support"          % scalaMockV     % "test"
  )
}
