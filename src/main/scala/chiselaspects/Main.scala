package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

object AspectMachine {
  def main(args: Array[String]): Unit = {

    val dir = sys.env.get("CHISELDIR").getOrElse("")

    if (!dir.isEmpty) {
      if(args.length != 0) {
        if(args(0) == "apply") {
          println("Applying features in " + dir)
          AspectManager(dir, DependencyChecker())
        } else if (args(0) == "undo"){
          println("Undo features in " + dir)
          AspectManager.undo(dir)
        } else {
          println("Please indicate either to apply or undo aspects!")
        }
      } else {
        println("Please indicate either to apply or undo aspects!")
      }
    } else {
      println("Plase set CHISELDIR to the location of your Chisel code!")
    }
  }
}
