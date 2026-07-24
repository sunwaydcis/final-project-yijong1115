ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "Project_23051725",

    libraryDependencies +=
      "org.scalafx" %% "scalafx" % "21.0.0-R32",

    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Wunused:all"
    ),

    fork := true
  )