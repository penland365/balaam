enablePlugins(JavaAppPackaging)

val buildName = "balaam"

name := buildName

version := "0.4.1"

organization := "codes.penland365"

scalaVersion := "2.12.4"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-language:existentials",
  "-Xlint",
  "-language:implicitConversions",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Ywarn-unused-import",
  "-Xfatal-warnings",
  "-Xfuture"
)

lazy val versions = new {
  val circe         = "0.8.0"
  val finagle       = "6.45.0"
  val twitterServer = "1.30.0"
  val scalatest     = "3.0.4"
  val scalamock     = "3.6.0"
  val finatra       = "2.11.0"
  val util          = "17.11.0"
  val storehaus     = "0.15.0"
  val slf           = "1.7.25"
  val roc           = "0.0.7-M1"
}

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest"                   % versions.scalatest,
  "org.scalamock" %% "scalamock-scalatest-support" % versions.scalamock
)


libraryDependencies ++= {
  Seq(
    "com.github.finagle"    %%  "roc-core"          %   versions.roc,
    "com.github.finagle"    %%  "roc-types"         %   versions.roc,
    "io.circe"              %%  "circe-core"        %   versions.circe,
    "io.circe"              %%  "circe-generic"     %   versions.circe,
    "io.circe"              %%  "circe-parser"      %   versions.circe,
    "com.twitter"           %%  "finagle-stats"     %   versions.finagle,
    "com.twitter"           %%  "finatra-http"      %   versions.finatra,
    "com.twitter"           %%  "twitter-server"    %   versions.twitterServer,
    "com.twitter"           %%  "util-slf4j-api"    %   versions.util,
    "com.twitter"           %%  "storehaus-cache"   %   versions.storehaus,
    "org.slf4j"             %   "slf4j-simple"      %   versions.slf
  )
}

resolvers ++= {
  Seq(
    "Twitter Maven repo" at "http://maven.twttr.com/"
  )
}
resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file(".")).
  settings(
    scalacOptions ++= compilerOptions,
    scalacOptions in (Compile, console) := compilerOptions,
    libraryDependencies ++= testDependencies.map(_ % "test"),
    name := buildName
)
