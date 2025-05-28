val scala3Version = "3.7.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "closetAssistant",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.18",
      "dev.zio" %% "zio-streams" % "2.1.18",
      "dev.zio" %% "zio-http" % "3.3.0"
    )
  )
