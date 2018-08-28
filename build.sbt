import java.io.File

import sbt.Keys.{name, resourceGenerators, version, _}
import sbt._
import sbtassembly.Plugin.AssemblyKeys._

val assembleZip = TaskKey[File]("assembleZip")
val zipArtifact = SettingKey[Artifact]("zipArtifact")

lazy val commonSettings = Seq(
  name := "elasticrypt",
  organization := "com.workday",
  version := "1.7.0-1",
  crossScalaVersions := Seq("2.10.4", "2.11.8")
)

lazy val root = Project(id = "elasticrypt", base = file("."))
  .settings(assemblySettings: _*)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.elasticsearch" % "elasticsearch" % "1.7.6-1" % "provided",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.scoverage" %% "scalac-scoverage-runtime" % "1.0.4", // Need this for integration test
      "org.mockito" % "mockito-all" % "1.9.5",
      "org.apache.httpcomponents" % "httpclient" % "4.4",
      "com.google.code.gson" % "gson" % "2.8.0"
    ),
    assembleZip <<= (assembly, target, name, version) map {
      (assembledJar: File, target: File, name: String, version: String) =>
        val artifact = target / s"$name-$version.zip"
        IO.write(target / "VERSION", version)

        val entries = Seq(
          (assembledJar, s"$name.jar"),
          (target / "VERSION", "VERSION")
        )

        IO.zip(entries, artifact)
        artifact
    },
    zipArtifact := Artifact(s"${name.value}", "zip", "zip"),

    // Use ScalaTest's built-in event buffering algorithm (shows events of 1 suite as they occur until suite completes/timeouts)
    logBuffered in Test := false,

    resourceGenerators in Compile <+=
      (resourceManaged in Compile, name, version) map { (dir, n, v) =>
        val file = dir / "elasticrypt.properties"
        val contents =
          s"""
             |plugin=org.elasticsearch.plugins.ElasticryptPlugin
             |version=$v
            """.stripMargin
        IO.write(file, contents)
        Seq(file)
      }
  )
  .settings(addArtifact(zipArtifact, assembleZip).settings: _*)
