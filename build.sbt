import java.util.Properties

val Scalaversion = "2.12.1"
val Scalatraversion = "2.5.1-NDLA-2"
val ScalaLoggingVersion = "3.5.0"
val Log4JVersion = "2.7"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.11.46"
val ScalaTestVersion = "3.0.1"
val MockitoVersion = "1.10.19"
val Elastic4sVersion = "5.2.8"
val ElasticsearchVersion = "5.1.1"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val commonSettings = Seq(
  organization := appProperties.value.getProperty("NDLAOrganization"),
  version := appProperties.value.getProperty("NDLAComponentVersion"),
  scalaVersion := Scalaversion
)

lazy val utlisting_api = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "utlisting-api",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.16",
      "joda-time" % "joda-time" % "2.8.2",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "org.json4s"   %% "json4s-native" % "3.5.0",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.scalikejdbc" %% "scalikejdbc" % "2.5.0",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4"
    )
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

unmanagedResourceDirectories in Compile <+= (baseDirectory) {_ / "src/main/webapp"}

assemblyJarName in assembly := "utlisting-api.jar"
mainClass in assembly := Some("no.ndla.utlistingapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")  => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")  => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class")  => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run on Travis as there is no elasticsearch localhost:9200 there yet. 
// NB this line will unfortunalty override runs on your local commandline so that
// sbt "test-only -- -n no.ndla.tag.IntegrationTest"
// will not run unless this line gets commented out or you remove the tag over the test class
// This should be solved better!
testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("java")

    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
  }
}

val gitHeadCommitSha = settingKey[String]("current git commit SHA")
gitHeadCommitSha in ThisBuild := Process("git log --pretty=format:%h -n 1").lines.head

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

parallelExecution in Test := false

resolvers ++= scala.util.Properties.envOrNone("NDLA_RELEASES").map(repo => "Release Sonatype Nexus Repository Manager" at repo).toSeq
