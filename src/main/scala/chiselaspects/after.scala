package chiselaspects

import scala.meta._
import scala.meta.contrib._

class After(oldCode: Stat, newCode: Stat = q"source()", context: Defn.Class = const.NullClass)(implicit aspect: Aspect)
  extends Advice(newCode, context) {

  def in(newContext: Defn.Class): Advice = {
    new After(oldCode, newCode, newContext)
  }

  def insert(newNewCode: Stat): Advice = {
    new After(oldCode, newNewCode, context)
  }

  def advise() = new Transformer {
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
      tree match {
        //note: ${insertAfter(bodyStats)} is a function call inside the quasiquote
        case template"{ ..$stats } with ..$inits { $self => ..$bodyStats }"
          if bodyStats.exists(_.isEqual(oldCode)) =>
            template"{ ..$stats } with ..$inits { $self => ..${insertAfter(bodyStats)} }"

        case q"{ ..$stats }" if stats.exists(_.isEqual(oldCode)) => q"{ ..${insertAfter(stats)} }"

        case q"new { ..$stat } with ..$inits { $self => ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            q"new { ..$stat } with ..$inits { $self => ..${insertAfter(stats)}}"

        case q"package $eref { ..$stats }" if stats.exists(_.isEqual(oldCode)) =>
          q"package $eref { ..${insertAfter(stats)} }"

        case source"..$stats" if stats.exists(_.isEqual(oldCode)) => source"..${insertAfter(stats)}"

        case _ => super.apply(tree)
      }
    }
  }

  private def insertAfter(bodyStats: List[Stat]): List[Stat] = bodyStats.flatMap(stat =>
    if (stat.isEqual(oldCode)) newCode match {
        case q"{ ..$stats }" => Seq(oldCode) ++ stats
        case _ => Seq(oldCode, newCode)
      }
    else Seq(stat)
  )
}
