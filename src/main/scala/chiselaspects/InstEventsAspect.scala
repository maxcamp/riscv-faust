package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class InstEventsAspect () extends Aspect {
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
  """) in (q"class RocketImpl") register
}
