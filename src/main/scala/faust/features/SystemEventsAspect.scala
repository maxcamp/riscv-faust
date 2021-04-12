package faust

import scala.meta._
import scala.meta.contrib._

class SystemEventsFeature extends Feature {

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
  val sysEvents = new EventSet((mask, hits) => (mask & hits).orR, 6)
  sysEvents.addEvent("ICache miss", () => io.imem.perf.acquire, 0)
  sysEvents.addEvent("DCache miss", () => io.dmem.acquire, 1)
  sysEvents.addEvent("DCache release", () => io.dmem.release, 2)
  sysEvents.addEvent("ITLB miss", () => io.imem.perf.tlbMiss, 3)
  sysEvents.addEvent("DTLB miss", () => io.dmem.tlbMiss, 4)
  sysEvents.addEvent("L2 TLB miss", () => io.ptw.perf.l2miss, 5)
  perfEvents.addEventSet(sysEvents)
  """) in (q"class RocketImpl") register
}
