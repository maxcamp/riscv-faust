package chiselaspects

import scalax.collection._
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scalax.collection.io.json._
import scalax.collection.io.json.descriptor.predefined.{Di}

import net.liftweb.json._

import scala.collection.immutable.Set
import scala.io.Source

sealed trait Chip
case class Feature(name: String) extends Chip

object DependencyChecker {
  private val featureDescriptor = new NodeDescriptor[Feature](typeId = "Features") {
    def id(node: Any) = node match {
      case Feature(name) => name
    }
  }

  private val chipDescriptor = new Descriptor[Chip](
    defaultNodeDescriptor = featureDescriptor,
    defaultEdgeDescriptor = Di.descriptor[Chip]()
  )

  private val root = Feature("Base System")

  private val featureFilename = "features.json"
  private val requestFilename = "request.json"

  private val featureImport = Source.fromFile(featureFilename).getLines.mkString
  private val dependencyGraph = Graph.fromJson[Chip, DiEdge](featureImport,chipDescriptor)

  private val requestImport = Source.fromFile(requestFilename).getLines.mkString
  private val requestGraph = Graph.fromJson[Chip, DiEdge](requestImport,chipDescriptor)

  //not very functional, but object oriented
  private val featureSet = scala.collection.mutable.Set[Feature]()

  def apply(): List[String] = {
    featureSet.clear()
    for(f <- requestGraph.nodes) {
      featureSet ++= getDependencies(f.toOuter.asInstanceOf[Feature])
    }

    for (f <- featureSet.toList) yield f.name
  }

  private def getDependencies(child: Feature): scala.collection.mutable.Set[Feature] = {
    //only go to the trouble of finding the dependencies if we haven't already seen them
    if(child != root && !featureSet.contains(child)) {
        //find our parents
        for(p <- (dependencyGraph get child).diSuccessors) {
          featureSet ++= getDependencies(p.toOuter.asInstanceOf[Feature])
        }
        //we depend on ourself, so add us too
        featureSet += child
    } else {
      featureSet += child
    }
  }

}
