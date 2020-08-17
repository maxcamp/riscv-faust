package transformer

import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import chisel3.aop.injecting.InjectingAspect
import scala.meta._

object TransformerMain extends App {
  val path = java.nio.file.Paths.get("/home/whytheam/Research/chisel/Adder/src/main/scala/adder", "Adder.scala")
  val bytes = java.nio.file.Files.readAllBytes(path)
  val text = new String(bytes, "UTF-8")
  val input = scala.meta.inputs.Input.VirtualFile(path.toString, text)
  val exampleTree = input.parse[Source].get
  val newText = exampleTree.transform {
    case q"new OneBitAdder()" => q"new OneBitAdder() with CarryLookaheadIO"
  }.toString
  println(newText)
}
