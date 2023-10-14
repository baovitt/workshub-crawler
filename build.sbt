import Dependencies._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val scrapers = (project in file("scrapers"))
  .settings(
    name := "workshub",
    libraryDependencies ++= Seq(
      "dev.doamaral" %% "zio-selenium" % "1.0.0",
      "io.github.bonigarcia" % "webdrivermanager" % "5.5.3",
      "org.seleniumhq.selenium" % "selenium-java" % "4.9.1",
      "org.seleniumhq.selenium" % "htmlunit-driver" % "4.9.1",
      "dev.zio" %% "zio-kafka" % "2.5.0",
      "io.getquill" %% "quill-zio" % "4.8.0",
      "dev.zio" %% "zio" % "2.0.9"
    )
  )
  .dependsOn(common)

lazy val consumer = (project in file("consumer"))
  .settings(
    name := "consumer",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-kafka" % "2.5.0",
      "dev.zio" %% "zio" % "2.0.9",
      "dev.zio" %% "zio-openai" % "0.2.1",
      "dev.zio" %% "zio-json" % "0.6.0",
      "io.weaviate" % "client" % "4.3.0",
      "com.google.guava" % "guava" % "32.1.2-jre"
    ),
    fork in run := true
  )
  .dependsOn(common)

lazy val common = (project in file("common"))
  .settings(
    name := "consumer",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % "0.6.0",
    ),
    fork in run := true
  )
