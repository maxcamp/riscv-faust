package transformer

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

object TransformerMain extends App {

  val dir = "/home/whytheam/Research/chisel/Transformer/src/main/scala/transformer/example"

  //aspects are applied in to out
  def aspectFunction (tree: Tree): Tree = {
    //new AroundBAspect(new AfterAAspect(tree)())()
    new AfterAAspect(new AroundBAspect(tree)())()
  }

  new AspectManager(dir, aspectFunction)()
}
