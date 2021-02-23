package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._
import java.nio.file.{Files, Paths, StandardCopyOption}

object AspectManager {

  def apply(dir: String)(aspectFunction: Tree => Tree) = {
    def getRecursiveListOfFiles(dir: File): Array[File] = {
      val these = dir.listFiles
      these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
    }

    def processTree(prevTree: Tree): Tree = {
      //val newTree =
        aspectFunction(prevTree)
      //if (prevTree.isEqual(newTree)) prevTree //once aspects stop applying return the tree
      //else processTree(newTree) //we have just applied aspects, need to check again
    }

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

      val originalTree = input.parse[Source].get

      val finalTree = processTree(originalTree)

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

  private def mv(source: String, destination: String): Unit = {
    val path = Files.move(
        Paths.get(source),
        Paths.get(destination),
        StandardCopyOption.REPLACE_EXISTING
    )
    // could return `path`
  }
}
