package faust

import scala.meta._
import scala.meta.contrib._

class MicroEventsFeature () extends Feature {
  after(q"val perfEvents = new EventSets()") insert (q"""
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
  """) in (q"class RocketImpl") register
}
