package faust

import scala.meta._
import scala.meta.contrib._

class PCCountersFeature extends Feature {

  before (q"buildMappings()") insert (q"var performanceCounters = new PCPerformanceCounters(perfEventSets, this, numPerfCounters)") in (q"class CSRFile") register
}
