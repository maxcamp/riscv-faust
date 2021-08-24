package faust

import scala.meta._
import scala.meta.contrib._

package object const {
  val NullClass = q"class __NotAClass"
  val NullInit = init"__NotAClass"
}

abstract class Advice(newCode: Tree, context: Defn.Class)
  (implicit feature: Feature) {

  //Outward facing interface

  //the subclass must tell us what to do with a new context
  def in(newConext: Defn.Class): Advice

  //the subclass must tell us how to handle new code to insert
  def insert(newNewCode: Tree): Advice

  //we must always tell the feature that the advice exists
  def register() = {
    feature.adviceList += advise
  }

  //Helper functions for advice application

  //finds the correct context
  protected def advise: Transformer

  //finds the correct position in the context
  protected def applyCode: Transformer
}
