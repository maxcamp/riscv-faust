// See README.md for license details.

def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // If we're building with Scala > 2.11, enable the compile option
    //  switch to support our anonymous Bundle definitions:
    //  https://github.com/scala/bug/issues/10047
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 => Seq()
      case _ => Seq("-Xsource:2.11")
    }
  }
}

def javacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // Scala 2.12 requires Java 8. We continue to generate
    //  Java 7 compatible code for Scala 2.11
    //  for compatibility with old clients.
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 =>
        Seq("-source", "1.7", "-target", "1.7")
      case _ =>
        Seq("-source", "1.8", "-target", "1.8")
    }
  }
}

name := "ChiselAspects"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions ++= scalacOptionsVersion(scalaVersion.value)
scalacOptions ++= Seq("-language:postfixOps")

javacOptions ++= javacOptionsVersion(scalaVersion.value)

libraryDependencies += "org.scalameta" %% "scalameta" % "4.3.20"

libraryDependencies += "org.scala-graph" %% "graph-core" % "1.13.2"

libraryDependencies += "org.scala-graph" %% "graph-constrained" % "1.13.2"

libraryDependencies += "org.scala-graph" %% "graph-json" % "1.13.0"

libraryDependencies += "org.scala-graph" %% "graph-dot" % "1.13.0"
