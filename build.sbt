name := "image_sharing_site_1"

version := "0.1"

scalaVersion := "2.12.10"

val AkkaVersion = "2.6.16"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % Test
