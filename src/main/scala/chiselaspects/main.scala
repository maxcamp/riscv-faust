package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

object AspectMachine {
  def main(args: Array[String]): Unit = {
    println("Applyin Aspects")
    AspectManager("/home/whytheam/Research/chisel/freedom/rocket-chip/src/main/scala")((tree: Tree) => new PerformanceCounterAspect(tree)())
  }
}
