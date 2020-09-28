package chiselaspects

import scala.meta._

class AroundBAspect (tree: Tree) extends Aspect(tree){

  val funcBJoinpoint = q"funcB"

  around(funcBJoinpoint, q"funcC")
}
