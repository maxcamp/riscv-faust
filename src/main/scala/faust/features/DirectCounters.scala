package faust

import scala.meta._
import scala.meta.contrib._

class DirectCountersFeature extends Feature {

  before (q"buildMappings()") insert (q"var performanceCounters = new DirectPerformanceCounters(perfEventSets, this, numPerfCounters)") in (q"class CSRFile") register
}
