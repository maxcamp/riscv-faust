#Faust: Feature Application Using Scala Trees#

**Faust** is a metaprogramming framework for applying features in Scala code. It provides
a small domain specific language for feature designation, feature dependency checking, and
automatic application of features to a code directory.

##Creating a new Feature##
To create a new feature in Faust, one first needs to extend the `Feature` class. A `Feature`
class represents one isolated feature that can be optionally applied to the code base.

###Feature Building Language###
Inside the `Feature` class a feature is built using a special syntax that we provide. A
feature is made of _advice_. Advice tells the framework where and what code needs to be
applied to code to implement the feature. There are three types of advice that can be
added to a feature: `before`, `after`, and `extend`. One of these keywords begin all advice.
All code must be wrapped in [quasiquotes](https://scalameta.org/docs/trees/quasiquotes.html)
as we operate upon _trees_ in the background.

Keyword | Use
------------ | -------------
before | Designates advice that will come before a statement.
after | Designates advice that will come after a statement.
extend | Designates advice that will extend a class.
insert | Designates what code will be inserted by the advice.
register | Designates the end of the advice statement.

**Example:**
```scala
package faust

import scala.meta._
import scala.meta.contrib._
import java.io.File
import java.io._

class CounterSystemFeature () extends Feature {
  val numPerfCounters = 28
  val haveBasicCounters = true

  //modifying Rocket Core
  val RocketCoreContext = q"class RocketImpl"
  after (q"hookUpCore()") insert (q"csr.io.counters foreach { c => c.inc := RegNext(perfEvents.evaluate(c.eventSel)) }") in RocketCoreContext register

  //modifying CSR
  val CSRContext = q"class CSRFile"

  val stat = q"${mod"override"} val counters = Vec(${numPerfCounters}, new PerfCounterIO)"
  extend (init"CSRFileIO") insert (q"{ $stat }") in CSRContext register

  before (q"buildMappings()") insert (q"val performanceCounters = new PerformanceCounters(perfEventSets, this, ${numPerfCounters}, ${haveBasicCounters})") in CSRContext register

  after(q"buildMappings()") insert q"performanceCounters.buildMappings()" in CSRContext register

  before (q"buildDecode()") insert (q"performanceCounters.buildDecode()") in CSRContext register
}
```

###Adding Dependencies###
Currently users must manually add the dependency relationships to the system. However, we
have plans to automate this in the future. New dependency information must be added to
`feature.json` and the correct class containing your custom feature must be added to
`DependencyChecher.scala`.

##Applying Features##
The directory you wish to apply features to must be designated in the enviroment variable
`SCALADIR`.

A particular set of features can be requested by adding them in `request.json`. To apply
features in [sbt](https://www.scala-sbt.org/) call `run apply`. To return your code to its
original state call `run undo`.

##Included Features##
Currently, our example features are for [Rocket Chip](https://github.com/chipsalliance/rocket-chip)
which is written in [Chisel](https://www.chisel-lang.org/). However, this framework _should_
work with any Scala code or any DSL embedded in Scala.

Currently we include the following features for Rocket Chip
* Base Performance Counter System
* Instruction Events
* Microarchitecture Events
* System Events
