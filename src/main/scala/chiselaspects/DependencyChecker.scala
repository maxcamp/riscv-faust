package chiselaspects

import scalax.collection._
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scalax.collection.io.json._
import scalax.collection.io.json.descriptor.predefined.{Di}

sealed trait Features
case class Feature(name: String) extends Features

class DependencyChecker() {

  val (baseSystem, instEvents, microEvents, sysEvents) = (
    Feature("Base System"),
    Feature("Instruction Events"),
    Feature("Microarchitecture Events"),
    Feature("System Events")
  )

  val featureGraph = Graph[Features, DiEdge] (
    instEvents ~> baseSystem,
    microEvents ~> baseSystem,
    sysEvents ~> baseSystem
  )

  val featureDescriptor = new NodeDescriptor[Feature](typeId = "Feature") {
    def id(node: Any) = node match {
      case Feature(name) => name
    }
  }

  val featuresDescriptor = new Descriptor[Features](
    defaultNodeDescriptor = featureDescriptor,
    defaultEdgeDescriptor = Di.descriptor[Features]()
  )

  def apply() {
    val export = featureGraph.toJson(featuresDescriptor)

    import net.liftweb.json._
    val pretty = prettyRender(JsonParser.parse(export))
    println(pretty)
  }

}
