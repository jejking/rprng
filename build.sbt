import sbt._
import ReleaseTransformations._

name := "rprng"
organization := "com.jejking"
scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.4.1"
  val akkaStreamV = "2.0"
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

lazy val rprng = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoObject := "Info",
    buildInfoPackage := "com.jejking.rprng.info"
  )

// releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishMavenStyle := true
publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
pomIncludeRepository := { _ => false }
// publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

pomExtra in Global := (
  <url>https://github.com/jejking/rprng</url>
    <licenses>
      <license>
        <name>Apache 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jejking/rprng.git</url>
      <connection>scm:git:git@github.com:jejking/rprng.git</connection>
    </scm>
    <developers>
      <developer>
        <id>jejking</id>
        <name>John King</name>
        <url>http://www.jejking.com</url>
      </developer>
    </developers>)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)