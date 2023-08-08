ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "Todo",
    idePackagePrefix := Some("dog.shebang")
  )

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.0",
)
