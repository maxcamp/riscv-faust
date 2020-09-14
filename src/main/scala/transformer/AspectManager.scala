package transformer

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class AspectManager (dir: String, aspectFunction: Tree => Tree) {
  private def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }

  private def processTree(prevTree: Tree): Tree = {
    //TODO: don't hard code this
    val newTree = aspectFunction(prevTree)
    if (prevTree.isEqual(newTree)) prevTree //once aspects stop applying return the tree
    else processTree(newTree) //we have just applied aspects, need to check again
  }

  //create the output directory
  private val outputDirName = System.getProperty("user.dir") + "/output"
  private val outputDir = new File(outputDirName)
  outputDir.mkdir

  //get all the files in the project we want to transform
  private val files = getRecursiveListOfFiles(new File(dir))

  //filter out all the non-scala files
  private val scalaFiles = files.filter(file =>
    file.getName match {
      case fileName: String if(fileName.endsWith(".scala")) => true
      case _ => false
    }
  )

  def apply() = {
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
        val newFile = new File(outputDirName + "/" + file.getName)
        val bw = new BufferedWriter(new FileWriter(newFile))
        bw.write(finalTree.syntax)
        bw.close()
        println("### Transform Applied in " + file.getName + " ###")
      }
    })
  }
}
