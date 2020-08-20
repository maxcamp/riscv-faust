package transformer

import scala.meta._
import scala.meta.contrib._


object TransformerMain extends App {
  val path = java.nio.file.Paths.get("/home/whytheam/Research/chisel/riscv-mini/src/main/scala", "Datapath.scala")
  val bytes = java.nio.file.Files.readAllBytes(path)
  val text = new String(bytes, "UTF-8")
  val input = scala.meta.inputs.Input.VirtualFile(path.toString, text)
  val sourceTree = input.parse[Source].get

  val insertModuleTree = Advice.before(q"val regFile = Module(new RegFile)", q"val instCounts = Module(new InstructionCounters)")(sourceTree)

  val finalTree = Advice.after(q"io.dcache.abort := csr.io.expt", q"instCounts.io.inst := ew_inst")(insertModuleTree)

  println(finalTree.syntax)
}
