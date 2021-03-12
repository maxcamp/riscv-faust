package chiselaspects

import scala.meta._

abstract class Aspect (){
  implicit val aspect = this
  val adviceList = scala.collection.mutable.ListBuffer[Transformer]()

  protected def before(oldCode: Stat) = new Before(oldCode)
  protected def after(oldCode: Stat) = new After(oldCode)
  protected def extend(oldCode: Init) = new ExtendInit(oldCode)
  protected def extend(oldCode: Defn.Class) = new ExtendClass(oldCode)

  def apply(tree: Tree): Tree = {
    adviceList.foldLeft(tree){ (newTree, transform) => { transform(newTree) } }
  }

}
