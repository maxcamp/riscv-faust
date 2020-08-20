package transformer

import scala.meta._

abstract class Aspect (tree: Tree){
  var adviceSequence = Seq[Transformer]()

  protected def addAdvice(advice: Transformer) {
    adviceSequence = adviceSequence :+ advice
  }

  def apply(): Tree = {
    adviceSequence.foldLeft(tree){
      (newTree, transform) => {
        transform(newTree)
      }
    }
  }
}
