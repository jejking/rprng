name := "zprng"
version := "0.1.0"
scalaVersion := "3.8.3"

val zioVersion = "2.1.25"

libraryDependencies ++= Seq(
    "dev.zio"         %% "zio"               % zioVersion,
    "dev.zio"         %% "zio-streams"       % zioVersion,
    "org.bouncycastle" % "bcprov-jdk18on"    % "1.78.1",
    "dev.zio"         %% "zio-test"          % zioVersion % Test,
    "dev.zio"         %% "zio-test-sbt"      % zioVersion % Test
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")