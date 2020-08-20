abstract class Aspect (tree: Tree){
  var adviceSequence = Seq[Transformer]()

  def addAdvice(advice: Transformer) {
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
