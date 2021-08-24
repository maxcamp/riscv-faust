package faust

import scala.meta._
import scala.meta.contrib._

class ExtendInit(oldCode: Init, newCode: Stat = q"source()", context: Defn.Class = const.NullClass)(implicit feature: Feature)
  extends Advice(newCode, context) {

  def in(newContext: Defn.Class): Advice = {
    new ExtendInit(oldCode, newCode, newContext)
  }

  def insert(newNewCode: Tree): Advice = {
    new ExtendInit(oldCode, newNewCode.asInstanceOf[Stat], context)
  }

  def advise = new Transformer {
    override def apply(tree: Tree): Tree = {
      tree match {
      case q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends $template"
        //if we've found the context OR we don't care about context, apply the code
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
      val q"{ ..$insertStats }" = newCode
      tree match {
        case q"new $init { ..$stats }" if (init.isEqual(oldCode)) => q"new $init { ..${insertStats ++ insertStats} }"
        case q"new $init" if (init.isEqual(oldCode)) => q"new $init { ..$insertStats }"
        case _ => super.apply(tree)
      }
    }
  }
}
