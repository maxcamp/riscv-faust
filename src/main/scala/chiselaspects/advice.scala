package chiselaspects

import scala.meta._
import scala.meta.contrib._
import scala.meta.Term.Block

object Advice {

  //Insert Single Statements

  def around(oldCode: Stat, newCode: Stat) = new Transformer {
      override def apply(tree: Tree): Tree = {
        //TODO: make around advide for a set of statements
        tree match {
          case q"..$mods def $oldCode[..$tparams](...$paramss): $tpeopt = $expr" => {
            val newExprTree = super.apply(expr)
            q"..$mods def $oldCode[..$tparams](...$paramss): $tpeopt = ${newExprTree.asInstanceOf[Term]}"
          }
          case _ => {
            if (tree.isEqual(oldCode)) {
              newCode
            } else {
              super.apply(tree)
            }
          }
        }
      }
  }

  def before(oldCode: Stat, newCode: Stat) = new Transformer {
    def insertBefore(bodyStats: List[Stat]): List[Stat] = bodyStats.flatMap(stat =>
      if (stat.isEqual(oldCode)) newCode match {
          case q"{ ..$stats }" => stats ++ Seq(oldCode)
          case _ => Seq(newCode, oldCode)
        }
      else Seq(stat)
    )

    override def apply(tree: Tree): Tree = {
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
  }

  //for replacing the type
  def after(oldInit: Init, newStats: Stat, last: Boolean = false) = new Transformer {
    override def apply(tree: Tree): Tree = {
      val q"{ ..$insertStats }" = newStats
      tree match {
        //match short circuits
        case q"new $init" if (init.isEqual(oldInit) && last) => q"new $init with NoAspect { ..$insertStats }"
        case q"new $init { ..$stats }" if (init.isEqual(oldInit) && last) => q"new $init with NoAspect { ..${insertStats ++ stats} }"
        case q"new $init" if (init.isEqual(oldInit)) => q"new $init { ..$insertStats }"
        case q"new $init { ..$stats }" if (init.isEqual(oldInit)) => q"new $init { ..${insertStats ++ stats} }"
        case _ => super.apply(tree)
      }
    }
  }

  def after(oldCode: Stat, newCode: Stat) = new Transformer {
    def insertAfter(bodyStats: List[Stat]): List[Stat] = bodyStats.flatMap(stat =>
      if (stat.isEqual(oldCode)) newCode match {
          case q"{ ..$stats }" => Seq(oldCode) ++ stats
          case _ => Seq(oldCode, newCode)
        }
      else Seq(stat)
    )

    override def apply(tree: Tree): Tree = {
      tree match {
        //note: ${insertAfter(bodyStats)} is a function call inside the quasiquote
        case template"{ ..$stats } with ..$inits { $self => ..$bodyStats }"
          if bodyStats.exists(_.isEqual(oldCode)) =>
            super.apply(template"{ ..$stats } with ..$inits { $self => ..${insertAfter(bodyStats)} }")

        case q"{ ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            super.apply(q"{ ..${insertAfter(stats)} }")

        case q"new { ..$stat } with ..$inits { $self => ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            super.apply(q"new { ..$stat } with ..$inits { $self => ..${insertAfter(stats)}}")

        case q"package $eref { ..$stats }"
          if stats.exists(_.isEqual(oldCode)) =>
            super.apply(q"package $eref { ..${insertAfter(stats)} }")

        case source"..$stats"
          if stats.exists(_.isEqual(oldCode)) =>
            super.apply(source"..${insertAfter(stats)}")

        case _ => super.apply(tree)
      }
    }
  }

}