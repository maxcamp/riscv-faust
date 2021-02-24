package chiselaspects

import scala.meta._

abstract class Aspect (tree: Tree){
  implicit val aspect = this
  var adviceSequence = Seq[Transformer]()

  protected def before(oldCode: Stat) = new Before(oldCode)
  protected def after(oldCode: Stat) = new After(oldCode)
  protected def extend(oldCode: Init) = new ExtendInit(oldCode)
  protected def extend(oldCode: Defn.Class) = new ExtendClass(oldCode)

  def apply(): Tree = {
    adviceSequence.foldLeft(tree){ (newTree, transform) => { transform(newTree) } }
  }

}
