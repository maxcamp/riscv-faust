package faust

import scala.meta._
import scala.meta.contrib._

class CounterSystemFeature () extends Feature {
  val numPerfCounters = 29

  //modifying Rocket Core
  val RocketCoreContext = q"class RocketImpl"
  after (q"hookUpCore()") insert (q"csr.io.counters foreach { c => c.inc := RegNext(perfEvents.evaluate(c.eventSel)) }") in RocketCoreContext register

  //modifying CSR
  val CSRContext = q"class CSRFile"

  val stat = q"${mod"override"} val counters = Vec(${numPerfCounters}, new PerfCounterIO)"
  extend (init"CSRFileIO") insert (q"{ $stat }") in CSRContext register

  before (q"buildMappings()") insert (q"val numPerfCounters = ${numPerfCounters}") in CSRContext register

  after (q"buildMappings()") insert q"performanceCounters.buildMappings()" in CSRContext register

  around (q"def allowCounter() = false.B") insert q"""
  val counter_addr = io_dec.csr(log2Ceil(performanceCounters.read_mcounteren.getWidth)-1, 0)
  def allowCounter() = (reg_mstatus.prv > PRV.S || performanceCounters.read_mcounteren(counter_addr)) && (!usingSupervisor || reg_mstatus.prv >= PRV.S || performanceCounters.read_scounteren(counter_addr))
  """ in CSRContext register

  before (q"buildDecode()") insert (q"performanceCounters.buildDecode()") in CSRContext register
}
