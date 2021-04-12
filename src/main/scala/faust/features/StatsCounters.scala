package faust

import scala.meta._
import scala.meta.contrib._

class StatsCountersFeature extends Feature {
  before (q"buildMappings()") insert (q"val statsCounters = new StatsCounters(performanceCounters, this)") in (q"class CSRFile") register 
}
