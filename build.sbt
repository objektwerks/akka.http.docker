enablePlugins(JavaAppPackaging, DockerPlugin, KubeDeploymentPlugin)

val dockerAppName = "now"
val dockerImageName = "akka-http-server"
val dockerHubName = "objektwerks"
val dockerAppVersion = "0.1"

name := "akka.http.docker.kubernetes"
organization := "objektwerks"
version := dockerAppVersion
scalaVersion := "2.13.10"
libraryDependencies ++= {
  val akkaVersion = "2.7.0"
  val akkaHttpVersion = "10.4.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe" % "config" % "1.4.2",
    "ch.qos.logback" % "logback-classic" % "1.4.3",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.14" % Test
  )
}

import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.packager.docker.DockerChmodType

Docker / packageName := dockerImageName
dockerExposedPorts ++= Seq(7979)
dockerBaseImage := "openjdk:8-jre-alpine"
dockerCommands ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
  ExecCmd("RUN", "apk", "add", "--no-cache", "curl")
)
dockerChmodType := DockerChmodType.UserGroupWriteExecute

import kubeyml.deployment._
import kubeyml.deployment.api._
import kubeyml.deployment.plugin.Keys._
import scala.concurrent.duration._
import scala.language.postfixOps

kube / namespace := "default"
kube / application := dockerAppName
kube / dockerImage := s"$dockerHubName/$dockerImageName:$dockerAppVersion"
kube / ports := List(Port(dockerAppName, 7979))
kube / envs := Map(
  EnvName("JAVA_OPTS") -> EnvRawValue("-Xms256M -Xmx1024M"),
)
kube / livenessProbe := HttpProbe(
  HttpGet(path = "/health", port = 7979, httpHeaders = List.empty),
  initialDelay = 3 seconds, timeout = 3 seconds, period = 30 seconds,
  failureThreshold = 3, successThreshold = 1
)
kube / resourceLimits := Resource(Cpu.fromCores(2), Memory(1024))
kube / resourceRequests := Resource(Cpu(500), Memory(512))
