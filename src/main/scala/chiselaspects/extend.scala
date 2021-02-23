package chiselaspects

import scala.meta._
import scala.meta.contrib._

class Extend(oldCode: Init, newCode: Stat = q"source()", context: Defn.Class = const.NullClass)(implicit aspect: Aspect)
  extends Advice(newCode, context) {

  def in(newContext: Defn.Class): Advice = {
    new Extend(oldCode, newCode, newContext)
  }

  def insert(newNewCode: Stat): Advice = {
    new Extend(oldCode, newNewCode, context)
  }

  def advise = new Transformer {
    override def apply(tree: Tree): Tree = {
      tree match {
      case q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends $template"
        if (tname.value == context.name.value || context.name.value == const.NullClass.name.value) => {
          val newTemplate: Template = applyCode(template).asInstanceOf[Template]
          q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends ${newTemplate}"
        }
      case _ => super.apply(tree)
     }
    }
  }

  private def applyCode = new Transformer {
    override def apply(tree: Tree): Tree = {
      val q"{ ..$insertStats }" = newCode
      tree match {
        //match short circuits
        //case q"new $init" if (init.isEqual(oldCode) && last) => q"new $init with NoAspect { ..$insertStats }"
        //case q"new $init { ..$stats }" if (init.isEqual(oldCode) && last) => q"new $init with NoAspect { ..${insertStats ++ stats} }"
        case q"new $init { ..$stats }" if (init.isEqual(oldCode)) => q"new $init { ..${insertStats ++ stats} }"
        case q"new $init" if (init.isEqual(oldCode)) => q"new $init { ..$insertStats }"
        case _ => super.apply(tree)
      }
    }
  }
}
