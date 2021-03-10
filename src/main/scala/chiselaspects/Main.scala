package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

object AspectMachine {
  def main(args: Array[String]): Unit = {

    val dir = "/home/whytheam/Research/chisel/freedom/rocket-chip/src/main/scala"

    if(args.length != 0) {
      if(args(0) == "apply") {
        println("Applying Aspects")
        AspectManager(dir)((tree: Tree) => new CounterSystemAspect(
          new InstEventsAspect(
            new MicroEventsAspect(
              new SystemEventsAspect(tree)())())())())
      } else if (args(0) == "undo"){
        println("Undo Aspects")
        AspectManager.undo(dir)
      } else if (args(0) == "depend"){
        DependencyChecker.check()
      } else {
        println("Please indicate either to apply or undo aspects!")
      }
    } else {
      println("Please indicate either to apply or undo aspects!")
    }
  }
}
