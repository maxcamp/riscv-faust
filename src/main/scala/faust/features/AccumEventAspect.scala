package faust

import scala.meta._
import scala.meta.contrib._

class AccumEventFeature extends Feature {

  //add IO to core
  extend (q"class CoreIO") insert (init"HasAccumEventIO") register

  //connect RoCC to Core IO
  after (q"connectRoCC()") insert (q"""
    outer.roccs.foreach(_ match {
    case acc: AccumulatorExample => core.io.accumEvent := acc.module.io.accumEvent
  })
  """) in (q"class RocketTileModuleImp") register

  //create new event set and event, add to core
  after(q"val perfEvents = new EventSets()") insert (q"""
  val accumEvents = new EventSet((mask, hits) => (mask & hits).orR, 1)
  accumEvents.addEvent("Accum", () => io.accumEvent, 0)
  perfEvents.addEventSet(accumEvents)
  """) in (q"class RocketImpl") register
}
