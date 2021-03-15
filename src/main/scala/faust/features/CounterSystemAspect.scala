package faust

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class CounterSystemFeature () extends Feature {
  val numPerfCounters = 28

  //modifying Rocket Core
  val RocketCoreContext = q"class RocketImpl"
  after (q"hookUpCore()") insert (q"csr.io.counters foreach { c => c.inc := RegNext(perfEvents.evaluate(c.eventSel)) }") in RocketCoreContext register

  //modifying CSR
  val CSRContext = q"class CSRFile"

  val stat = q"${mod"override"} val counters = Vec(${numPerfCounters}, new PerfCounterIO)"
  extend (init"CSRFileIO") insert (q"{ $stat }") in CSRContext register

  before (q"buildMappings()") insert (q"val performanceCounters = new PerformanceCounters(perfEventSets, this, ${numPerfCounters})") in CSRContext register

  after(q"buildMappings()") insert q"performanceCounters.buildMappings()" in CSRContext register

  before (q"buildDecode()") insert (q"performanceCounters.buildDecode()") in CSRContext register
}
