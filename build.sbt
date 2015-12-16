import sbt._

import sbtrelease._
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}
import com.typesafe.sbt.SbtGit.GitKeys._

name := "rprng"
organization := "com.jejking"
scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

git.useGitDescribe := true
git.baseVersion := "1.0.0"
val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r
git.gitTagToVersionNumber := {
  case VersionRegex(v,"") => Some(v)
  case VersionRegex(v,"SNAPSHOT") => Some(s"$v-SNAPSHOT")
  case VersionRegex(v,s) => Some(s"$v-$s-SNAPSHOT")
  case _ => None
}
git.gitDescribedVersion := gitReader.value.withGit(_.describedVersion).flatMap(v =>
  Option(v).map(_.drop(1)).orElse(formattedShaVersion.value).orElse(Some(git.baseVersion.value))
)
showCurrentGitBranch


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

lazy val rprng = (project in file(".")).
  enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoObject := "Info",
    buildInfoPackage := "com.jejking.rprng.info"
  )


publishMavenStyle := true
publishArtifact in Test := false
/*
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
pomIncludeRepository := { _ => false }
*/
publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

pomExtra := (
  <description>A reactive PRNG web service</description>
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

def setVersionOnly(selectVersion: Versions => String): ReleaseStep =  { st: State =>
  val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
  val selected = selectVersion(vs)

  st.log.info("Setting version to '%s'." format selected)
  val useGlobal =Project.extract(st).get(releaseUseGlobalVersion)
  val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

  reapply(Seq(
    if (useGlobal) version in ThisBuild := selected
    else version := selected
  ), st)
}

lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

releaseVersion <<= (releaseVersionBump)( bumper=>{
   ver => Version(ver)
          .map(_.withoutQualifier)
          .map(_.bump(bumper).string).getOrElse(versionFormatError)
})

val showNextVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
val showReleaseVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
showReleaseVersion <<= (version, releaseVersion)((v,f)=>f(v))
showNextVersion <<= (version, releaseNextVersion)((v,f)=>f(v))

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  runTest,
  tagRelease,
  publishArtifacts,
  //ReleaseStep(releaseStepTask(publish in Universal)),
  pushChanges
)
