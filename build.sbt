import sbt._

name := "rprng"
organization := "com.jejking"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.4.1"
  val akkaStreamV = "2.0-M2"
  val scalaTestV  = "2.2.5"
  val commonsMathV = "3.5"
  val scalaMockV = "3.2.2"
  val logbackV = "1.1.3"
  Seq(
    "org.apache.commons" % "commons-math3"                        % commonsMathV,
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-slf4j"                           % akkaV,
    "ch.qos.logback"    % "logback-classic"                       % logbackV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV % "test",
    "com.typesafe.akka" %% "akka-stream-testkit-experimental"     % akkaStreamV % "test",
    "org.scalatest"     %% "scalatest"                            % scalaTestV  % "test",
    "com.typesafe.akka" %% "akka-testkit"                         % akkaV       % "test",
    "org.scalamock"     %% "scalamock-core"                       % scalaMockV  % "test",
    "org.scalamock"     %% "scalamock-scalatest-support"          % scalaMockV  % "test"
  )
}

mainClass in assembly := Some("com.jejking.rprng.api.Main")