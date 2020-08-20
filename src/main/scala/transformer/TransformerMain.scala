package transformer

import scala.meta._
import scala.meta.contrib._

class InstructionCounterAspect (tree: Tree) extends Aspect (tree){

  val moduleCreationJoinpoint = q"val regFile = Module(new RegFile)"
  val ioConnectionJoinpoint = q"io.dcache.abort := csr.io.expt"

  addAdvice(Advice.before(moduleCreationJoinpoint, q"val instCounts = Module(new InstructionCounters)"))

  addAdvice(Advice.after(ioConnectionJoinpoint, q"instCounts.io.inst := ew_inst"))
}


object TransformerMain extends App {
  val path = java.nio.file.Paths.get("/home/whytheam/Research/chisel/riscv-mini/src/main/scala", "Datapath.scala")
  val bytes = java.nio.file.Files.readAllBytes(path)
  val text = new String(bytes, "UTF-8")
  val input = scala.meta.inputs.Input.VirtualFile(path.toString, text)
  implicit val tree = input.parse[Source].get

  println(new InstructionCounterAspect(tree)().syntax)
}
