enablePlugins(JavaAppPackaging)

val buildName = "balaam"

name := buildName

version := "0.0.1-M1"

organization := "codes.penland365"

scalaVersion := "2.12.3"

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
//  "-Xfatal-warnings",
  "-Xfuture"
)

lazy val versions = new {
  val circe         = "0.8.0"
  val finagle       = "6.45.0"
  val twitterServer = "1.30.0"
  val scalatest     = "3.0.4"
  val scalamock     = "3.6.0"
  val finatra       = "2.11.0"
}

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest"                   % versions.scalatest,
  "org.scalamock" %% "scalamock-scalatest-support" % versions.scalamock
)


libraryDependencies ++= {
  Seq(
    "io.circe"      %%  "circe-core"        %   versions.circe,
    "io.circe"      %%  "circe-generic"     %   versions.circe,
    "io.circe"      %%  "circe-parser"      %   versions.circe,
    "com.twitter"   %%  "finagle-stats"     %   versions.finagle,
    "com.twitter"   %%  "finatra-http"      %   versions.finatra,
    "com.twitter"   %%  "twitter-server"    %   versions.twitterServer
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

lazy val generatedDest = "src/generated/scala"
lazy val genProto = taskKey[Unit]("Generates gRPC files from 'unum/protos/poss.proto'")
lazy val genConvoProto = taskKey[Unit]("Generates gRPC files from 'unum/protos/convo.proto")

genProto := {
  s"rm -rf $generatedDest"!

  s"mkdir -p $generatedDest"!

  s"protoc -I ../protos ../protos/poss.proto --io.buoyant.grpc_out=plugins=grpc:$generatedDest"!
}

genConvoProto := {
  s"protoc -I ../protos ../protos/convo.proto --io.buoyant.grpc_out=plugins=grpc:$generatedDest"!
}

unmanagedSourceDirectories in Compile += baseDirectory.value / generatedDest
