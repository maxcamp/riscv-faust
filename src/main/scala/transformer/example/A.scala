package transformer

object A {
  def funcA = {
    println("Hello from A")
    B.funcB
  }

  funcA
}
