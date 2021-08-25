package faust

import scala.meta._
import scala.meta.contrib._

class Around(oldCode: Stat, newCode: Stat = q"source()", context: Defn.Class = const.NullClass)(implicit feature: Feature)
  extends StatementAdvice(oldCode, newCode, context) {

  def in(newContext: Defn.Class): Advice = {
    new Around(oldCode, newCode, newContext)
  }

  def insert(newNewCode: Tree): Advice = {
    new Around(oldCode, newNewCode.asInstanceOf[Stat], context)
  }

  def doInsert(bodyStats: List[Stat]): List[Stat] = bodyStats.flatMap(stat =>
    if (stat.isEqual(oldCode)) newCode match {
        case q"{ ..$stats }" => stats
        case _ => Seq(newCode)
      }
    else Seq(stat)
  )
}
