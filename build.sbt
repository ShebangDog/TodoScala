ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

(Test / scalaSource) := (Compile / scalaSource).value

reStart / mainClass := Some("dog.shebang.Main")

lazy val root = (project in file("."))
  .settings(
    name := "Todo",
    idePackagePrefix := Some("dog.shebang")
  )

val hedgehogVersion = "0.10.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.0",
  "io.github.iltotore" %% "iron" % "2.1.0",
  "org.scalatest" %% "scalatest-diagrams" % "3.2.15",
  "org.scalatest" %% "scalatest-funspec" % "3.2.15",
  "qa.hedgehog" %% "hedgehog-core" % hedgehogVersion,
  "qa.hedgehog" %% "hedgehog-runner" % hedgehogVersion,
  "qa.hedgehog" %% "hedgehog-sbt" % hedgehogVersion
)
