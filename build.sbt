import com.github.retronym.SbtOneJar._

oneJarSettings

name         := "semserv"

version      := "0.1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.hermit-reasoner" % "org.semanticweb.hermit" % "1.3.8.4"

libraryDependencies += "io.spray" % "spray-json_2.11" % "1.3.3"

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

mainClass in (Compile, run) := Some("Main")
