package chiselaspects

import scalax.collection._
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scalax.collection.io.json._
import scalax.collection.io.json.descriptor.predefined.{Di}

import net.liftweb.json._


sealed trait Chip
case class Feature(name: String) extends Chip

class DependencyChecker() {

  val (baseSystem, counterSystem, instEvents, microEvents, sysEvents) = (
    Feature("Base System"),
    Feature("Counter System"),
    Feature("Instruction Events"),
    Feature("Microarchitecture Events"),
    Feature("System Events")
  )

  val featureGraph = Graph[Chip, DiEdge] (
    counterSystem ~> baseSystem,
    instEvents ~> counterSystem,
    microEvents ~> counterSystem,
    sysEvents ~> counterSystem
  )

  val featureDescriptor = new NodeDescriptor[Feature](typeId = "Features") {
    def id(node: Any) = node match {
      case Feature(name) => name
    }
  }

  val chipDescriptor = new Descriptor[Chip](
    defaultNodeDescriptor = featureDescriptor,
    defaultEdgeDescriptor = Di.descriptor[Chip](),
    namedNodeDescriptors = Seq(featureDescriptor),
    namedEdgeDescriptors = Seq(Di.descriptor[Chip]())
  )

  def apply() {
    val export = featureGraph.toJson(chipDescriptor)
    val formated = prettyRender(JsonParser.parse(export))
    println(formated)

    val newGraph = Graph.fromJson[Chip, DiEdge](export, chipDescriptor)
    println(newGraph.toString())
  }

}
