package at.wambo.renderer

import scalafx.scene.paint.Color
import akka.actor.{Actor, Props, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import concurrent.Future
import akka.pattern.ask
import scala.collection.mutable.ArrayBuffer

/*
 * User: Martin
 * Date: 08.11.13
 * Time: 13:15
 */

class ParallelRayTracer(val screenWidth: Int, val screenHeight: Int, numThreads: Int) extends Renderer {
  val system = ActorSystem("renderer")
  implicit val timeout = Timeout(15 seconds)
  private val colsPerThread = screenHeight / numThreads


  // TODO when an actor is done with its part of the rendering, send him a new part of the scene to render
  def render(scene: Scene): Future[Array[Color]] = {
    val rayTracer = new RayTracer(screenWidth, screenHeight, true)
    val children = (for (i <- 0 until numThreads)
    yield system.actorOf(Props(classOf[RenderActor], rayTracer, scene))).zipWithIndex
    val futures = for {(child, idx) <- children
                       startY = colsPerThread * idx
                       endY = colsPerThread * (idx + 1)} yield {
      child ? Render((0, startY), (screenWidth, endY))
    }

    val buffer = ArrayBuffer.empty[(Array[Color], Int)]
    // import system.dispatcher
    // TODO Accumulate results in buffer
    // TODO Make sure buffer is in the right order (either sort by start index or do something else)
    // TODO return the above as a Future

    ???
  }

  def close() {
    system.shutdown()
  }
}

case class Render(startPos: (Int, Int), endPos: (Int, Int))

case class Finished(time: Long, start: (Int, Int), end: (Int, Int), data: Array[Color])

class RenderActor(rt: RayTracer, scene: Scene) extends Actor {
  def receive = {
    case Render(start, end) => {
      val data = rt.render(scene, start, end)
      sender ! Finished(System.nanoTime(), start, end, data)
    }
  }
}