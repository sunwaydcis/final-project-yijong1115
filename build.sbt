ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "Project_23051725",

    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "21.0.0-R32",
      "org.apache.derby" % "derby" % "10.17.1.0"
    ),

    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Wunused:all"
    ),

    fork := true
  )