package faust

import scala.meta._
import scala.meta.contrib._

class StatsCountersFeature extends Feature {
  val numStatsCounters = 28

  before (q"buildMappings()") insert (q"val performanceCounters = new StatisticalPerformanceCounters(perfEventSets, this, numPerfCounters, ${numStatsCounters})") in (q"class CSRFile") register
}
