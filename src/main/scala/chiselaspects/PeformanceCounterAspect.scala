package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class PerformanceCounterAspect (tree: Tree) extends Aspect(tree) {

  val numPerfCounters = 28
  val haveBasicCounters = true

  //modifying CSR
  val stat = q"${mod"override"} val counters = Vec(${numPerfCounters}, new PerfCounterIO)"
  after(init"CSRFileIO", q"{ $stat }")

  before(q"buildMappings()", q"val performanceCounters = new PerformanceCounters(perfEventSets, this, ${numPerfCounters}, ${haveBasicCounters})")

  after(q"buildMappings()", q"performanceCounters.buildMappings()")

  before(q"buildDecode()", q"performanceCounters.buildDecode()")

  //modifying Rocket Core
  after(q"val perfEvents = new EventSets()", q"""

  val instEvents = new EventSet((mask, hits) => Mux(wb_xcpt, mask(0), wb_valid &&
    pipelineIDToWB((mask & hits).orR)), 18)
  instEvents.addEvent("exception", () => false.B, 0)
  instEvents.addEvent("load", () => id_ctrl.mem && id_ctrl.mem_cmd === M_XRD
    && !id_ctrl.fp, 1)
  instEvents.addEvent("store", () => id_ctrl.mem && id_ctrl.mem_cmd === M_XWR &&
    !id_ctrl.fp, 2)
  instEvents.addEvent("amo", () => Bool(usingAtomics) && id_ctrl.mem &&
    (isAMO(id_ctrl.mem_cmd) || id_ctrl.mem_cmd.isOneOf(M_XLR, M_XSC)), 3)
  instEvents.addEvent("system", () => id_ctrl.csr =/= CSR.N, 4)
  instEvents.addEvent("arith", () => id_ctrl.wxd && !(id_ctrl.jal || id_ctrl.jalr ||
    id_ctrl.mem || id_ctrl.fp || id_ctrl.mul || id_ctrl.div || id_ctrl.csr =/= CSR.N), 5)
  instEvents.addEvent("branch", () => id_ctrl.branch, 6)
  instEvents.addEvent("jal", () => id_ctrl.jal, 7)
  instEvents.addEvent("jalr", () => id_ctrl.jalr, 8)
  if (usingMulDiv) {
    instEvents.addEvent("mul", () => if (pipelinedMul) id_ctrl.mul else id_ctrl.div &&
      (id_ctrl.alu_fn & ALU.FN_DIV) =/= ALU.FN_DIV, 9)
    instEvents.addEvent("div", () => if (pipelinedMul) id_ctrl.div else id_ctrl.div &&
      (id_ctrl.alu_fn & ALU.FN_DIV) === ALU.FN_DIV, 10)
  }
  if (usingFPU) {
    instEvents.addEvent("fp load", () => id_ctrl.fp && io.fpu.dec.ldst && io.fpu.dec.wen, 11)
    instEvents.addEvent("fp store", () => id_ctrl.fp && io.fpu.dec.ldst && !io.fpu.dec.wen, 12)
    instEvents.addEvent("fp add", () => id_ctrl.fp && io.fpu.dec.fma && io.fpu.dec.swap23, 13)
    instEvents.addEvent("fp mul", () => id_ctrl.fp && io.fpu.dec.fma && !io.fpu.dec.swap23
      && !io.fpu.dec.ren3, 14)
    instEvents.addEvent("fp mul-add", () => id_ctrl.fp && io.fpu.dec.fma && io.fpu.dec.ren3, 15)
    instEvents.addEvent("fp div/sqrt", () => id_ctrl.fp && (io.fpu.dec.div || io.fpu.dec.sqrt), 16)
    instEvents.addEvent("fp other", () => id_ctrl.fp && !(io.fpu.dec.ldst || io.fpu.dec.fma
      || io.fpu.dec.div || io.fpu.dec.sqrt), 17)
  }
  perfEvents.addEventSet(instEvents)

  val microEvents = new EventSet((mask, hits) => (mask & hits).orR, 11)
  microEvents.addEvent("load-use interlock", () => id_ex_hazard && ex_ctrl.mem
    || id_mem_hazard && mem_ctrl.mem || id_wb_hazard && wb_ctrl.mem, 0)
  microEvents.addEvent("long-latency interlock", () => id_sboard_hazard, 1)
  microEvents.addEvent("csr interlock", () => id_ex_hazard && ex_ctrl.csr =/= CSR.N
    || id_mem_hazard && mem_ctrl.csr =/= CSR.N || id_wb_hazard && wb_ctrl.csr =/= CSR.N, 2)
  microEvents.addEvent("ICache blocked", () => icache_blocked, 3)
  microEvents.addEvent("DCache blocked", () => id_ctrl.mem && dcache_blocked, 4)
  microEvents.addEvent("branch misprediction", () => take_pc_mem && mem_direction_misprediction, 5)
  microEvents.addEvent("control-flow target misprediction", () => take_pc_mem && mem_misprediction
    && mem_cfi && !mem_direction_misprediction && !icache_blocked, 6)
  microEvents.addEvent("flush", () => wb_reg_flush_pipe, 7)
  microEvents.addEvent("replay", () => replay_wb, 8)
  if (usingMulDiv) {
    microEvents.addEvent("mul/div interlock", () => id_ex_hazard && (ex_ctrl.mul || ex_ctrl.div)
      || id_mem_hazard && (mem_ctrl.mul || mem_ctrl.div) || id_wb_hazard && wb_ctrl.div, 9)
  }
  if (usingFPU) {
    microEvents.addEvent("fp interlock", () => id_ex_hazard && ex_ctrl.fp || id_mem_hazard && mem_ctrl.fp
      || id_wb_hazard && wb_ctrl.fp || id_ctrl.fp && id_stall_fpu, 10)
  }
  perfEvents.addEventSet(microEvents)

  val sysEvents = new EventSet((mask, hits) => (mask & hits).orR, 6)
  sysEvents.addEvent("ICache miss", () => io.imem.perf.acquire, 0)
  sysEvents.addEvent("DCache miss", () => io.dmem.perf.acquire, 1)
  sysEvents.addEvent("DCache release", () => io.dmem.perf.release, 2)
  sysEvents.addEvent("ITLB miss", () => io.imem.perf.tlbMiss, 3)
  sysEvents.addEvent("DTLB miss", () => io.dmem.perf.tlbMiss, 4)
  sysEvents.addEvent("L2 TLB miss", () => io.ptw.perf.l2miss, 5)
  perfEvents.addEventSet(sysEvents)
  """)

  after(q"hookUpCore()", q"csr.io.counters foreach { c => c.inc := RegNext(perfEvents.evaluate(c.eventSel)) }")
}
