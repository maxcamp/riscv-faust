package faust

import scala.meta._
import scala.meta.contrib._

abstract class StatementAdvice(oldCode: Stat, newCode: Tree, context: Defn.Class)
  (implicit feature: Feature) extends Advice(newCode, context) {

  //Helper functions for advice application

  //finds the correct context
  protected def advise = new Transformer {
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

  //finds the correct position in the context
  protected def applyCode = new Transformer {
    override def apply(tree: Tree): Tree = {
      tree match {
        //note: ${doInsert(bodyStats)} is a function call inside the quasiquote
        case template"{ ..$stats } with ..$inits { $self => ..$bodyStats }"
          if bodyStats.exists(_.isEqual(oldCode)) =>
            template"{ ..$stats } with ..$inits { $self => ..${doInsert(bodyStats)} }"

        case q"{ ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            q"{ ..${doInsert(stats)} }"

        case q"new { ..$stat } with ..$inits { $self => ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            q"new { ..$stat } with ..$inits { $self => ..${doInsert(stats)}}"

        case q"package $eref { ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            q"package $eref { ..${doInsert(stats)} }"

        case source"..$stats"
          if stats.exists(_.isEqual(oldCode)) =>
            source"..${doInsert(stats)}"

        case _ => super.apply(tree)
      }
    }
  }

  //subclass must implment this to customize its advice
  protected def doInsert(code: List[Stat]): List[Stat]
}
