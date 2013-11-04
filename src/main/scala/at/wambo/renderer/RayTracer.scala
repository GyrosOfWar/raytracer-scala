package at.wambo.renderer

import scalafx.scene.paint.Color
import akka.actor._
import scalafx.scene.image.PixelWriter
import akka.util.Timeout
import concurrent.duration._
import akka.pattern.ask

/*
 * User: Martin
 * Date: 30.10.13
 * Time: 23:40
 */

class RayTracer(val setPixel: (Int, Int, Color) => Unit, size: (Int, Int)) {
  private val maxDepth = 5
  private val backgroundColor = Vec.Zero
  private val defaultColor = Vec.Zero
  private val (screenWidth, screenHeight) = size

  private def intersections(ray: Ray, scene: Scene) = scene.things.flatMap(_.intersect(ray)).sortBy(_.distance)

  private def testRay(ray: Ray, scene: Scene) =
    intersections(ray, scene).headOption match {
      case Some(intersection) => intersection.distance
      case None => 0.0
    }

  private def getNaturalColor(thing: SceneObject, pos: Vec, norm: Vec, reflectDir: Vec, scene: Scene): Vec = {
    (for (light <- scene.lights) yield {
      val lightDir = light.position - pos
      val lightVec = lightDir.normalize
      val is = testRay(Ray(start = pos, direction = lightVec), scene)
      val isInShadow = !((is > lightDir.magnitude) || is == 0)
      if (!isInShadow) {
        val illum = lightVec.dot(norm)
        val lightColor = if (illum > 0) light.color * illum else Vec.Zero
        val specular = lightVec.dot(reflectDir.normalize)
        val specularColor = if (specular > 0) light.color * math.pow(specular, thing.surface.roughness) else Vec.Zero
        thing.surface.diffuse(pos) * lightColor + thing.surface.specular(pos) * specularColor
      }
      else {
        backgroundColor
      }
    }).reduce((v, w) => v + w)
  }

  private def getReflectionColor(thing: SceneObject, pos: Vec, norm: Vec, reflectDir: Vec, scene: Scene, depth: Int): Vec = {
    traceRay(Ray(pos, reflectDir), scene, depth + 1) * thing.surface.reflect(pos)
  }

  private def shade(intersection: Intersection, scene: Scene, depth: Int): Vec = {
    val d = intersection.ray.direction
    val pos = intersection.ray.direction * intersection.distance + intersection.ray.start
    val normal = intersection.thing.normal(pos)
    val reflectDir = d - ((normal * normal.dot(d)) * 2)
    val ret = defaultColor + getNaturalColor(intersection.thing, pos, normal, reflectDir, scene)
    if (depth >= maxDepth)
      ret + Vec(0.5, 0.5, 0.5)
    else {
      ret + getReflectionColor(intersection.thing, pos + reflectDir * 0.001, normal, reflectDir, scene, depth)
    }

  }

  private def traceRay(ray: Ray, scene: Scene, depth: Int): Vec =
    intersections(ray, scene).headOption match {
      case Some(intersection) => shade(intersection, scene, depth)
      case None => backgroundColor
    }

  private def recenterX(x: Double): Double = (x - (screenWidth / 2.0)) / (2.0 * screenWidth)

  private def recenterY(y: Double): Double = -(y - (screenHeight / 2.0)) / (2.0 * screenHeight)

  private def getPoint(x: Double, y: Double, camera: Camera): Vec = {
    (camera.forward + (camera.right * recenterX(x) + camera.up * recenterY(y))).normalize
  }

  def render(scene: Scene, startPos: (Int, Int), endPos: (Int, Int)) {
    val (startX, startY) = startPos
    val (endX, endY) = endPos

    for (y <- startY until endY) {
      for (x <- startX until endX) {
        val color = traceRay(Ray(start = scene.camera.position,
          direction = getPoint(x, y, scene.camera)), scene, 0)
        setPixel(x, y, color.toScalaFxColor)
      }
    }
  }
}

class ParallelRayTracer(writer: PixelWriter, scene: Scene, imageSize: (Int, Int), numThreads: Int) {
  val system = ActorSystem("renderer")
  val (width, height) = imageSize
  implicit val timeout = Timeout(15 seconds)
  private val colsPerThread = height / numThreads

  def render() = {
    val children = for (i <- 0 until numThreads)
      yield system.actorOf(Props(classOf[RenderActor], writer, imageSize, scene))
    val ret = for ((child, idx) <- children.zipWithIndex) yield {
      val startY = colsPerThread * idx
      val endY = colsPerThread * (idx + 1)
      (child ? Render((0, startY), (width, endY))).mapTo[Long]
    }

    for(c <- children) system stop c
    ret
  }

  def close() {
    system.shutdown()
  }
}

case class Render(startPos: (Int, Int), endPos: (Int, Int))

case class Finished(time: Long)

class RenderActor(writer: PixelWriter, imageSize: (Int, Int), scene: Scene) extends Actor {
  def receive = {
    case Render(start, end) => {
      val rt = new RayTracer(writer.setColor, imageSize)
      rt.render(scene, start, end)
      sender ! Finished(System.nanoTime())
    }
  }
}