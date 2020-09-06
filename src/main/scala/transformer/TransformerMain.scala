package transformer

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class InstructionCounterAspect (tree: Tree) extends Aspect (tree){

  val CSRJoinpoint = init"CSRFile(perfEvents, coreParams.customCSRs.decls)"
  val coreJoinpoint = init"Rocket(outer)(outer.p)"

  val CSRExtension = q"""
    override lazy val io = new CSRFileIO {
      val customCSRs = Vec(coreParams.customCSRs.decls.size, new CustomCSRIO).asOutput
      val flushInstructionCounters = Output(Bool())
      flushInstructionCounters := false.B
    }

    override def generateCustomCSRs {
      val reg_flushInstructionCounters = new CSreg(0x800, Reg(UInt(xLen.W))) ((reg: Data) => {
        //kind of magic, but we don't need super
        when(csr_wen) {
          reg := wdata
          io.flushInstructionCounters := true.B
        }
      })
    }"""

    val coreExtenstion = q"""
    //create a new piece of hardware
    val instructionCounters = Module(new InstructionCounters(rocketImpl.decode_table))
    //wire up all the signals we need
    instructionCounters.io.inst := io.trace(0).insn
    instructionCounters.io.valid := io.trace(0).valid
    //define some new behavior to control the hardware
    when(rocketImpl.csr.io.flushInstructionCounters) {
      instructionCounters.reset := true.B
    }"""

  around(CSRJoinpoint, CSRExtension)
  around(coreJoinpoint, coreExtenstion)

}


object TransformerMain extends App {

  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }

  val dir = "/home/whytheam/Research/chisel/rocket-chip/src/main/scala"

  //create the output directory
  val outputDirName = System.getProperty("user.dir") + "/output"
  val outputDir = new File(outputDirName)
  outputDir.mkdir

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
    implicit val tree = input.parse[Source].get
    val newTree = new InstructionCounterAspect(tree)()

    //if we've done a transform, write a new file with the new tree
    if (!newTree.isEqual(tree)) {
      val newFile = new File(outputDirName + "/" + file.getName)
      val bw = new BufferedWriter(new FileWriter(newFile))
      //TODO: Don't hard code this
      bw.write(new InstructionCounterAspect(tree)().syntax)
      bw.close()
      println("### Transform Applied in " + file.getName + " ###")
    }
  })
}
