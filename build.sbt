val scala3Version = "3.7.0"
val zioAwsVersion = "7.28.29.13"

run / fork := true
lazy val root = project
  .in(file("."))
  .settings(
    name := "closetAssistant",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.18",
      "dev.zio" %% "zio-streams" % "2.1.18",
      "dev.zio" %% "zio-http" % "3.3.0",
      "dev.zio" %% "zio-aws-core" % zioAwsVersion,
      "dev.zio" %% "zio-aws-netty" % zioAwsVersion,
      "dev.zio" %% "zio-aws-s3" % zioAwsVersion,
      "dev.zio" %% "zio-dynamodb" % "1.0.0-RC19",
      "org.slf4j" % "slf4j-api" % "2.0.16",
      "org.slf4j" % "slf4j-simple" % "2.0.16"
    )
  )
