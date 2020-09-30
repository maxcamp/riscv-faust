package chiselaspects

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class InstructionCounterAspect (tree: Tree) extends Aspect (tree){

  val CSRJoinpoint = init"CSRFile(perfEvents, coreParams.customCSRs.decls)"
  val coreJoinpoint = init"Rocket(outer)(outer.p)"

  val CSRExtension = q"""
    override lazy val io = new CSRFileIO {
      val customCSRs = Vec(coreParams.customCSRs.decls.size, new CustomCSRIO).asOutput
      val flushInstructionCounters = Output(Bool())
      flushInstructionCounters := false.B
    }

    override def generateCustomCSRs {
      val reg_flushInstructionCounters = new CSreg(0x800, Reg(UInt(xLen.W))) ((reg: Data) => {
        //kind of magic, but we don't need super
        when(csr_wen) {
          reg := wdata
          io.flushInstructionCounters := true.B
        }
      })
    }"""

    val coreExtenstion = q"""
    //create a new piece of hardware
    val instructionCounters = Module(new InstructionCounters(rocketImpl.decode_table))
    //wire up all the signals we need
    instructionCounters.io.inst := io.trace(0).insn
    instructionCounters.io.valid := io.trace(0).valid
    //define some new behavior to control the hardware
    when(rocketImpl.csr.io.flushInstructionCounters) {
      instructionCounters.reset := true.B
    }"""

  around(CSRJoinpoint, CSRExtension)
  around(coreJoinpoint, coreExtenstion)

}
