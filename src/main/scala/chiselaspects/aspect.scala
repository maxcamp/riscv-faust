package chiselaspects

import scala.meta._

abstract class Aspect (tree: Tree){
  var adviceSequence = Seq[Transformer]()

  private def addAdvice(advice: Transformer) {
    adviceSequence = adviceSequence :+ advice
  }

  protected def around (oldCode: Stat, newCode: Stat) = {
    addAdvice(Advice.around(oldCode, newCode))
  }

  protected def before (oldCode: Stat, newCode: Stat) = {
    addAdvice(Advice.before(oldCode, newCode))
  }

  protected def after(oldInit: Init, newStats: Stat, last: Boolean = false) = {
    addAdvice(Advice.after(oldInit, newStats, last))
  }

  protected def after (oldCode: Stat, newCode: Stat) = {
    addAdvice(Advice.after(oldCode, newCode))
  }

  def apply(): Tree = {
    adviceSequence.foldLeft(tree){ (newTree, transform) => { transform(newTree) } }
  }

}
