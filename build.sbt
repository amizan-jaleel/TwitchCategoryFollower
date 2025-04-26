import sbt.Keys.*
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*
import play.sbt.PlayImport.ws // If you need Play WS client
import play.sbt.routes.RoutesKeys.* // For Play routes compiler
import sbt.*

// --- Common Settings ---
val scala2Version = "2.13.16" // <-- UPDATED Scala version
val organizationName = "org.amizanjaleel"
val projectName = "TwitchCategoryFollower"
val playVersion     = "2.9.3"
val playJsonVersion = playVersion

lazy val commonSettings = Seq(
  organization := organizationName,
  version := "1.0.0-SNAPSHOT",
  scalaVersion := scala2Version,
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding", "UTF-8",
    "-Xfatal-warnings",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps"
    // For Scala 2.13.16, you might consider options like:
    // "-Wunused:imports", // Warn if imports are unused (standard in Scala 3)
    // "-Wvalue-discard", // Warn when non-Unit results are discarded
  )
)

// --- Project Definitions ---
// Root project: aggregates others, doesn't contain code itself
lazy val root = (project in file("."))
  .aggregate(server, client, sharedJVM, sharedJS)
  .settings(
    commonSettings,
    name := projectName,
    publish / skip := true
  )

// Shared code project (cross-compiled for JVM and JS)
lazy val shared = (crossProject(JSPlatform, JVMPlatform) in file("shared"))
  .settings(
    commonSettings,
    name := "shared"
  )
  .settings(
    libraryDependencies ++= Seq(
      // Using Circe as an example cross-platform JSON library
      // Ensure versions are compatible with Scala 2.13.16
      "io.circe" %%% "circe-core"    % "0.14.9", // Check latest 0.14.x for Scala 2.13
      "io.circe" %%% "circe-generic" % "0.14.9",
      "io.circe" %%% "circe-parser"  % "0.14.9",
      "com.typesafe.play" %%% "play-json" % playJsonVersion,
    )
  )
  .jvmSettings( /* JVM-specific shared settings/deps */ )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0" // Stable version
  )

// Shortcuts for shared modules
lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

// Server project (Play Framework - JVM)
lazy val server = (project in file("server"))
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(sharedJVM)
  .settings(
    commonSettings,
    name := "server",
    libraryDependencies ++= Seq(
      guice,
      ws % Test,
      // Add other backend dependencies
      // "com.typesafe.play" %% "play-json" % "2.9.4" // Or check latest 2.9.x/2.10.x compatible with Play 2.8
      "com.dripower" %% "play-circe" % "2814.1", // Check latest play-circe version matching your Play & Circe versions
),
    routesGenerator := InjectedRoutesGenerator,
    // Task to copy Scala.js output to Play's public assets folder
    Compile / resourceGenerators += Def.task {
      // Using fastOptJS output for development linking
      val jsFile = (client / Compile / fastOptJS / artifactPath).value
      val sourceMap = jsFile.toPath.resolveSibling(jsFile.name + ".map").toFile

      val targetDir = (Compile / classDirectory).value / "public" / "javascripts"
      IO.copyFile(jsFile, targetDir / jsFile.name)

      // Copy source map only if it exists
      if (sourceMap.exists()) {
        IO.copyFile(sourceMap, targetDir / sourceMap.name)
        Seq(targetDir / jsFile.name, targetDir / sourceMap.name)
      } else {
        Seq(targetDir / jsFile.name)
      }
    }.taskValue
  )

// Client project (Scala.js)
lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS) // Depends on shared code (includes DataModel.Item + Circe implicits)
  .settings(
    commonSettings,
    name := "client",
    scalaJSUseMainModuleInitializer := true, // Must be true to run main method
    Compile / mainClass := Some("org.amizanjaleel.client.MainApp"), // Set your main class
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0", // For DOM manipulation and Ajax
      // Circe dependencies are likely inherited via sharedJS if defined there with %%%
      // but add parser explicitly if not already included:
      "io.circe" %%% "circe-parser" % "0.14.1" // Needed for decode() - align version
    ),
    // Configure output JS file path (used by server copy task and index.html)
    Compile / fastOptJS / artifactPath := (ThisBuild / baseDirectory).value / "server" / "public" / "javascripts" / s"${name.value}-fastopt.js",
    Compile / fullOptJS / artifactPath := (ThisBuild / baseDirectory).value / "server" / "public" / "javascripts" / s"${name.value}-opt.js",

    // Optional: configure source map base URL for better debugging in browser dev tools
    // This helps the browser find original Scala sources if served correctly
    scalacOptions += {
      val hostedPath = s"http://localhost:9000/assets/javascripts/${name.value}-fastopt.js.map" // Adjust port/path if needed
      val localPath = (Compile / fastOptJS / artifactPath).value.toURI.toString
      s"-P:scalajs:mapSourceURI:$localPath->$hostedPath"
    }
  )

//ThisBuild / version := "0.1.0-SNAPSHOT"
//
//ThisBuild / scalaVersion := "2.13.16"
//
//lazy val root = (project in file("."))
//  .settings(
//    name := "TwitchCategoryFollower",
//    idePackagePrefix := Some("org.amizan-jaleel")
//  )
