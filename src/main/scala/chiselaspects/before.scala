package chiselaspects

import scala.meta._
import scala.meta.contrib._


class Before(oldCode: Stat, newCode: Stat = q"source()", context: Defn.Class = const.NullClass)(implicit aspect: Aspect)
  extends Advice(oldCode: Stat, newCode: Stat, context: Defn.Class) {

  def in(newContext: Defn.Class): Advice = {
    new Before(oldCode, newCode, newContext)
  }

  def insert(newNewCode: Stat): Advice = {
    new Before(oldCode, newNewCode, context)
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

    def applyCode(tree: Tree): Tree = {
      tree match {
        //note: ${insertBefore(bodyStats)} is a function call inside the quasiquote
        case template"{ ..$stats } with ..$inits { $self => ..$bodyStats }"
          if bodyStats.exists(_.isEqual(oldCode)) =>
            template"{ ..$stats } with ..$inits { $self => ..${insertBefore(bodyStats)} }"

        case q"{ ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            q"{ ..${insertBefore(stats)} }"

        case q"new { ..$stat } with ..$inits { $self => ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            q"new { ..$stat } with ..$inits { $self => ..${insertBefore(stats)}}"

        case q"package $eref { ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            q"package $eref { ..${insertBefore(stats)} }"

        case source"..$stats"
          if stats.exists(_.isEqual(oldCode)) =>
            source"..${insertBefore(stats)}"

        case _ => super.apply(tree)
      }
    }

    def insertBefore(bodyStats: List[Stat]): List[Stat] = bodyStats.flatMap(stat =>
      if (stat.isEqual(oldCode)) newCode match {
          case q"{ ..$stats }" => stats ++ Seq(oldCode)
          case _ => Seq(newCode, oldCode)
        }
      else Seq(stat)
    )
  }

}
