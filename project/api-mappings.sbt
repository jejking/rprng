addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "latest.release")

apiMappings += (
  (unmanagedBase.value / "commons-math3-3.6.jar") ->
    url("http://commons.apache.org/proper/commons-math/javadocs/api-3.6/")
  )