package transformer

import scala.meta._

abstract class Aspect (tree: Tree){
  var adviceSequence = Seq[Transformer]()

  private def addAdvice(advice: Transformer) {
    adviceSequence = adviceSequence :+ advice
  }

  protected def around (oldCode: Stat, newCode: Stat) = {
    addAdvice(Advice.around(oldCode, newCode))
  }

  protected def around(oldInit: Init, newStats: Stat) = {
    addAdvice(Advice.around(oldInit, newStats))
  }

  protected def before (oldCode: Stat, newCode: Stat) = {
    addAdvice(Advice.before(oldCode, newCode))
  }

  protected def after (oldCode: Stat, newCode: Stat) = {
    addAdvice(Advice.after(oldCode, newCode))
  }

  def apply(): Tree = {
    adviceSequence.foldLeft(tree){ (newTree, transform) => { transform(newTree) } }
  }

}
