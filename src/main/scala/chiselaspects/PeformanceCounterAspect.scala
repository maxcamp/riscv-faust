package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class PerformanceCounterAspect (tree: Tree) extends Aspect(tree) {

  val numPerfCounters = 28
  val haveBasicCounters = true

  //modifying PTW
  extend (q"class DatapathPTWIO") insert (init"HasPTWPerfEvents") register

  //these are too fragile, but fine for now
  after (q"l2_refill_wire := l2_refill") insert (q"""
    io.dpath.perf.l2miss := false
    io.dpath.perf.l2hit := false
  """) in (q"class PTW") register

  //these are too fragile, but fine for now
  before (q"val s2_pte = Wire(new PTE)") insert (q"""
    io.dpath.perf.l2miss := s2_valid && !(s2_hit_vec.orR)
    io.dpath.perf.l2hit := s2_hit
  """) in (q"class PTW") register

  after (q"buildControlStateMachine()") insert (q"""
    io.dpath.perf.pte_miss := false
    io.dpath.perf.pte_hit := pte_hit && (state === s_req) && !io.dpath.perf.l2hit
    assert(!(io.dpath.perf.l2hit && (io.dpath.perf.pte_miss || io.dpath.perf.pte_hit)),
    "PTE Cache Hit/Miss Performance Monitor Events are lower priority than L2TLB Hit event")

    if (state == s_wait2) {
      io.dpath.perf.pte_miss := count < pgLevels-1
    }
  """) in (q"class PTW") register

  //modifying ICache
  extend (q"class ICacheBundle") insert (init"HasICachePerfEvents") register

  after (q"buildRefill()") insert (q"io.perf.acquire := refill_fire") in (q"class ICacheModule") register

  //modifying NDBCache
  before (q"gateClock()") insert (q"""
    io.cpu.acquire := edge.done(tl_out.a)
    io.cpu.release := edge.done(tl_out.c)
    io.cpu.tlbMiss := io.ptw.req.fire()
  """) in (q"class NonBlockingDCacheModule") register

  //modifying HellaCacheArbiter
  after (q"connectRequestorToMem()") insert (q"""
    for (i <- 0 until n) {
      io.requestor(i).perf := io.mem.perf
    }
  """) in (q"class HellaCacheArbiter") register

  //modifying HellaCacheIO
  extend (q"class HellaCacheIO") insert (init"HasHellaCachePerfEvents") register

  //modifying Frontend
  extend (q"class FrontendIO") insert (init"HasFrontEndPerfEvents") register

  before (q"gateClock()") insert (q"""
    io.cpu.perf := icache.io.perf
    io.cpu.perf.tlbMiss := io.ptw.req.fire()
    io.errors := icache.io.errors
  """) in (q"class FrontendModule") register

  //modifying DCache
  after(q"gateClock()") insert (q"""
    io.cpu.perf.storeBufferEmptyAfterLoad := !(
      (s1_valid && s1_write) ||
      ((s2_valid && s2_write && !s2_waw_hazard) || pstore1_held) ||
      pstore2_valid)
    io.cpu.perf.storeBufferEmptyAfterStore := !(
      (s1_valid && s1_write) ||
      (s2_valid && s2_write && pstore1_rmw) ||
      ((s2_valid && s2_write && !s2_waw_hazard || pstore1_held) && pstore2_valid))
    io.cpu.perf.canAcceptStoreThenLoad := !(
      ((s2_valid && s2_write && pstore1_rmw) && (s1_valid && s1_write && !s1_waw_hazard)) ||
      (pstore2_valid && pstore1_valid_likely && (s1_valid && s1_write)))
    io.cpu.perf.canAcceptStoreThenRMW := io.cpu.perf.canAcceptStoreThenLoad && !pstore2_valid
    io.cpu.perf.canAcceptLoadThenLoad := !((s1_valid && s1_write && needsRead(s1_req)) && ((s2_valid && s2_write && !s2_waw_hazard || pstore1_held) || pstore2_valid))
  """) in (q"class DCacheModuleImpl") register

  //modifying Rocket Core
  after(q"val perfEvents = new EventSets()") insert (q"""
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
  sysEvents.addEvent("DCache miss", () => io.dmem.acquire, 1)
  sysEvents.addEvent("DCache release", () => io.dmem.release, 2)
  sysEvents.addEvent("ITLB miss", () => io.imem.perf.tlbMiss, 3)
  sysEvents.addEvent("DTLB miss", () => io.dmem.tlbMiss, 4)
  sysEvents.addEvent("L2 TLB miss", () => io.ptw.perf.l2miss, 5)
  perfEvents.addEventSet(sysEvents)
  """) in (q"class RocketImpl") register

  after (q"hookUpCore()") insert (q"csr.io.counters foreach { c => c.inc := RegNext(perfEvents.evaluate(c.eventSel)) }") in (q"class RocketImpl") register

  //modifying CSR
  val stat = q"${mod"override"} val counters = Vec(${numPerfCounters}, new PerfCounterIO)"
  extend (init"CSRFileIO") insert (q"{ $stat }") in (q"class CSRFile") register

  before (q"buildMappings()") insert (q"val performanceCounters = new PerformanceCounters(perfEventSets, this, ${numPerfCounters}, ${haveBasicCounters})") in (q"class CSRFile") register

  after(q"buildMappings()") insert q"performanceCounters.buildMappings()" in (q"class CSRFile") register

  before (q"buildDecode()") insert (q"performanceCounters.buildDecode()") in (q"class CSRFile") register
}
