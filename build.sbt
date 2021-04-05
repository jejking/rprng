import sbt._
import ReleaseTransformations._

resolvers += Resolver.sonatypeRepo("public")

enablePlugins(DockerPlugin)

name := "rprng"
organization := "com.jejking"
scalaVersion := "2.13.5"
autoAPIMappings := true

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "utf8",
  "-feature", "-language:postfixOps",
  "-target:11")

libraryDependencies ++= {
  val akkaV        = "2.6.13"
  val akkaHttpV    = "10.2.4"
  val scalaTestV   = "3.2.5"
  val commonsMathV = "3.6.1"
  val scalaMockV   = "5.1.0"
  val logbackV     = "1.2.3"

  Seq(
    "org.apache.commons"    % "commons-math3"                         % commonsMathV,
    "com.typesafe.akka"     %% "akka-actor"                           % akkaV,
    "com.typesafe.akka"     %% "akka-slf4j"                           % akkaV,
    "ch.qos.logback"        % "logback-classic"                       % logbackV,
    "ch.qos.logback"        % "logback-core"                          % logbackV,
    "net.logstash.logback"  % "logstash-logback-encoder"              % "6.6",
    "com.typesafe.akka"     %% "akka-stream"                          % akkaV,
    "com.typesafe.akka"     %% "akka-http-core"                       % akkaHttpV,
    "com.typesafe.akka"     %% "akka-http"                            % akkaHttpV,
    "com.typesafe.akka"     %% "akka-http-spray-json"                 % akkaHttpV,
    "com.typesafe.akka"     %% "akka-http-testkit"                    % akkaHttpV   % "test",
    "com.typesafe.akka"     %% "akka-stream-testkit"                  % akkaV       % "test",
    "org.scalatest"         %% "scalatest"                            % scalaTestV  % "test",
    "com.typesafe.akka"     %% "akka-testkit"                         % akkaV       % "test",
    "org.scalamock"         %% "scalamock"                            % scalaMockV  % "test"
  )
}

test in assembly := {}
mainClass in assembly := Some("com.jejking.rprng.main.Main")

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

imageNames in docker := Seq(

  // Sets a name with a tag that contains the project version
  ImageName(
    registry = Some("docker.io"),
    namespace = Some("jejking"),
    repository = name.value,
    tag = Some("v" + version.value)
  ),
  ImageName(
    registry = Some("docker.io"),
    namespace = Some("jejking"),
    repository = name.value,
    tag = Some("latest")
  )

)

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:alpine")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
    expose(8080)
  }
}
