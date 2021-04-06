package faust

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._
import java.nio.file.{Files, Paths, StandardCopyOption}

object FeatureManager {

  def apply(dir: String, featureList: List[String]) = {

    //gather the features that have been requested
    val features = getFeatures(featureList)

    //get all the files in the project we want to transform
    val files = getRecursiveListOfFiles(new File(dir))

    //filter out all the non-scala files
    val scalaFiles = files.filter(file =>
      file.getName match {
        case fileName: String if(fileName.endsWith(".scala")) => true
        case _ => false
      }
    )
    //apply the transformation to every file we've found
    scalaFiles.foreach(file => {
      println("Processing " + file.getName + " ...")
      val path = file.toPath
      val bytes = java.nio.file.Files.readAllBytes(path)
      val text = new String(bytes, "UTF-8")
      val input = scala.meta.inputs.Input.VirtualFile(path.toString, text)

      val originalTree = input.parse[Source].get.asInstanceOf[Tree]

      //take feature n and apply it's transform to tree n-1
      val finalTree = (features.foldLeft(originalTree)
        ((tree: Tree, feature: Feature) => feature(tree)))

      //if we've done a transform, write a new file with the new tree
      if (!finalTree.isEqual(originalTree)) {
        mv(path.toString, path + "_orig".toString)

        val newFile = new File(path.toString)
        val bw = new BufferedWriter(new FileWriter(newFile))
        bw.write(finalTree.syntax)
        bw.close()
        println("### Transform Applied in " + file.getName + " ###")
      }
    })
  }

  def undo(dir: String) = {
    //get all the files in the project we want to undo
    val files = getRecursiveListOfFiles(new File(dir))

    //find all the original files
    val origFiles = files.filter(file =>
      file.getName match {
        case fileName: String if(fileName.endsWith(".scala_orig")) => true
        case _ => false
      }
    )

    //process all the files
    origFiles.foreach(file => {
      println("Undo " + file.getName + " ...")
      val path = file.toString
      val newPath = path.substring(0, path.length - 5)

      mv(path, newPath)
    })

  }

  private def getFeatures(featureList: List[String]): List[Feature] = {
    for (f <- featureList) yield {
      f match {
        case "Accum Event" => new AccumEventFeature()
        case "Counter System" => new CounterSystemFeature()
        case "Instruction Events" => new InstEventsFeature()
        case "Microarchitecture Events" => new MicroEventsFeature()
        case "System Events" => new SystemEventsFeature()
        case "Base System" => new BaseSystemFeature()
      }
    }
  }

  private def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }

  private def mv(source: String, destination: String): Unit = {
    val path = Files.move(
        Paths.get(source),
        Paths.get(destination),
        StandardCopyOption.REPLACE_EXISTING
    )
    // could return `path`
  }
}
