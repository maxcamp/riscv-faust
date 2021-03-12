package faust

import scala.meta._
import scala.meta.contrib._

package object const {
  val NullClass = q"class __NotAClass"
  val NullInit = init"__NotAClass"
}

abstract class Advice(newCode: Tree, context: Defn.Class)
  (implicit feature: Feature) {

  //the subclass must tell us what to do with a new context
  def in(newConext: Defn.Class): Advice

  //the subclass must tell us how to handle new code to insert
  def insert(newNewCode: Tree): Advice

  //the subclass must tell us how to advise
  def advise(): Transformer

  //we must always tell the feature that the advice exists
  def register() = {
    feature.adviceList += advise()
  }
}
