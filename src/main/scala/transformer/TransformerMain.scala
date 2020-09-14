package transformer

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

object TransformerMain extends App {

  val dir = "/home/whytheam/Research/chisel/rocket-chip/src/main/scala"

  def aspectFunction (tree: Tree): Tree = {
    new InstructionCounterAspect(tree)()
  }

  new AspectManager(dir, aspectFunction)()
}
