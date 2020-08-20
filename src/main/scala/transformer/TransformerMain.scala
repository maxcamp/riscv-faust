package transformer

import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import chisel3.aop.injecting.InjectingAspect
import scala.meta._
import scala.meta.contrib._


object TransformerMain extends App {

  def around(oldCode: Tree, newCode: Tree) = new Transformer {
    override def apply(tree: Tree): Tree = {
      if (tree.isEqual(oldCode)) {
        newCode
      } else {
        super.apply(tree)
      }
    }
  }

  def before(oldCode: Stat, newCode: Stat) = new Transformer {
    override def apply(tree: Tree): Tree = {
      tree match {
        case template"{ ..$stats } with ..$inits { $self => ..$bodyStats }" => {
          val newBodyStats = bodyStats.flatMap(stat =>
            if (stat.isEqual(oldCode))
              Seq(newCode, oldCode)
            else Seq(stat)
          )
          template"{ ..$stats } with ..$inits { $self => ..$newBodyStats }"
        }
        case _ => super.apply(tree)
      }
    }
  }

  def after(oldCode: Stat, newCode: Stat) = new Transformer {
    override def apply(tree: Tree): Tree = {
      tree match {
        case template"{ ..$stats } with ..$inits { $self => ..$bodyStats }" => {
          val newBodyStats = bodyStats.flatMap(stat =>
            if (stat.isEqual(oldCode))
              Seq(oldCode, newCode)
            else Seq(stat)
          )
          template"{ ..$stats } with ..$inits { $self => ..$newBodyStats }"
        }
        case _ => super.apply(tree)
      }
    }
  }

  val path = java.nio.file.Paths.get("/home/whytheam/Research/chisel/riscv-mini/src/main/scala", "Datapath.scala")
  val bytes = java.nio.file.Files.readAllBytes(path)
  val text = new String(bytes, "UTF-8")
  val input = scala.meta.inputs.Input.VirtualFile(path.toString, text)
  val sourceTree = input.parse[Source].get

  val insertModuleTree = before(q"val regFile = Module(new RegFile)", q"val instCounts = Module(new InstructionCounters)")(sourceTree)

  val finalTree = after(q"io.dcache.abort := csr.io.expt", q"instCounts.io.inst := ew_inst")(insertModuleTree)

  println(finalTree.syntax)
}
