import com.github.retronym.SbtOneJar._

oneJarSettings

name         := "semserv"

version      := "0.1.0"

scalaVersion := "2.11.8"

resolvers += "emueller-bintray" at "http://dl.bintray.com/emueller/maven"

libraryDependencies += "com.hermit-reasoner" % "org.semanticweb.hermit" % "1.3.8.4"

libraryDependencies += "com.eclipsesource" %% "play-json-schema-validator" % "0.8.8"

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

mainClass in (Compile, run) := Some("Main")
