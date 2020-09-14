package transformer

import scala.meta._

class AfterAAspect (tree: Tree) extends Aspect(tree){

  val funcAJoinpoint = q"funcA"

  after(funcAJoinpoint, q"B.funcB")
}
