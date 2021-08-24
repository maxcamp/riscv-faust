package faust

import scala.meta._
import scala.meta.contrib._

class ExtendClass(oldCode: Defn.Class, newCode: Init = const.NullInit, context: Defn.Class = const.NullClass)
  (implicit feature: Feature) extends Advice(newCode, context) {

  def in(newContext: Defn.Class): Advice = {
    new ExtendClass(oldCode, newCode, newContext)
  }

  def insert(newNewCode: Tree): Advice = {
    new ExtendClass(oldCode, newNewCode.asInstanceOf[Init], context)
  }

  def advise = new Transformer {
    override def apply(tree: Tree): Tree = {
      tree match {
      //if there's no context to identify a subclass to modify, direcly modify class
      case q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends $template"
        if (tname.value == oldCode.name.value && context.name.value == const.NullClass.name.value) => applyCode(tree)
      //if we've found the context class, pass the rest of the tree for modification
      case q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends $template"
        if (tname.value == context.name.value || context.name.value == const.NullClass.name.value) => {
          val newTemplate: Template = applyCode(template).asInstanceOf[Template]
          q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends ${newTemplate}"
        }
      case _ => super.apply(tree)
     }
    }
  }

  def applyCode = new Transformer {
    override def apply(tree: Tree): Tree = {
      tree match {
        case q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends $oldTemplate"
          if (tname.value == oldCode.name.value) => {
            //break apart the template
            val template"{ ..$stats } with ..$inits { $self => ..$bodyStats }" = oldTemplate
            //build a new template with the new init that we want
            val newTemplate = template"{ ..$stats } with ..${inits ++ Seq(newCode)} { $self => ..$bodyStats }"

            q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends $newTemplate"
          }
        case _ => super.apply(tree)
      }
    }
  }
}
