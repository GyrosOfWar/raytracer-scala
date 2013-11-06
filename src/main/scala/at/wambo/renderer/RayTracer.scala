package at.wambo.renderer

import scalafx.scene.paint.Color
import akka.actor._
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
  private val samplingPattern = List(Vec(0, 0.25), Vec(0, -0.25), Vec(0.25, 0), Vec(-0.25, 0))

  /**
   * Intersects a ray with a scene.
   * @param ray The ray to use in the intersection
   * @param scene The scene to test against
   * @return The list of intersections of the ray, sorted by their distance to the ray origin.
   */
  private def intersections(ray: Ray, scene: Scene) =
    (for {
      thing <- scene.things
      intersection <- thing.intersect(ray)
    } yield intersection) sortBy (_.distance)

  /**
   * Computes the intersections of a ray and a scene and returns the distance of the first object hit.
   * @param ray Ray to test
   * @param scene Scene to test
   * @return The distance of the first object that was hit by the ray or 0 if there were no intersections.
   */
  private def testRay(ray: Ray, scene: Scene): Double =
    intersections(ray, scene).headOption match {
      case Some(intersection) => intersection.distance
      case None => 0.0
    }

  /**
   * This is called for every object intersection and applies shading and lighting to the object at the point
   * of the intersection. The arguments are all at the point of the reflection.
   * @param thing SceneObject to use
   * @param pos World position of the SceneObject at the intersection
   * @param norm Normal vector at the intersection
   * @param reflectDir reflection direction at the point of the intersection
   * @param scene Scene to test
   * @return Color after applying all of the lights and shading
   */
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


  /**
   * Given a SceneObject and its position, normal and reflection direction, computes the reflection color at
   * an intersection with a ray.
   * @param thing SceneObject
   * @param pos World position
   * @param norm Normal vector
   * @param reflectDir Reflection direction
   * @param scene ..
   * @param depth The current recursion depth
   * @return Color of the reflected surface.
   */
  private def getReflectionColor(thing: SceneObject, pos: Vec, norm: Vec, reflectDir: Vec, scene: Scene, depth: Int): Vec = {
    traceRay(Ray(pos, reflectDir), scene, depth + 1) * thing.surface.reflect(pos)
  }

  /**
   * Does the complete shading for one intersection, with reflections and normal shading.
   * @param intersection The intersection, consisting of the SceneObject, the ray and the distance.
   * @param scene Scene to use.
   * @param depth Current recursion depth.
   * @return Color of the point at which the intersection occurred.
   */
  private def shade(intersection: Intersection, scene: Scene, depth: Int): Vec = {
    val d = intersection.ray.direction
    val pos = d * intersection.distance + intersection.ray.start
    val normal = intersection.thing.normal(pos)
    val reflectDir = d - ((normal * normal.dot(d)) * 2)
    val ret = defaultColor + getNaturalColor(intersection.thing, pos, normal, reflectDir, scene)
    if (depth >= maxDepth)
      ret + Vec(0.5, 0.5, 0.5)
    else {
      ret + getReflectionColor(intersection.thing, pos + reflectDir * 0.001, normal, reflectDir, scene, depth)
    }
  }

  /**
   * Traces one ray. First computes the intersections of the ray, then computes its shading, or otherwise returns
   * the background color (if no intersections were found).
   * @param ray Ray to trace
   * @param scene Scene to test against
   * @param depth Current recursion depth
   * @return Color of the ray at the given position.
   */
  private def traceRay(ray: Ray, scene: Scene, depth: Int): Vec =
    Util.timedCall("render", printTime = false) {
      intersections(ray, scene).headOption match {
        case Some(intersection) => shade(intersection, scene, depth)
        case None => backgroundColor
      }
    }

  private def recenterX(x: Double): Double = (x - (screenWidth / 2.0)) / (2.0 * screenWidth)

  private def recenterY(y: Double): Double = -(y - (screenHeight / 2.0)) / (2.0 * screenHeight)

  private def getPoint(x: Double, y: Double, camera: Camera): Vec =
    (camera.forward + (camera.right * recenterX(x) + camera.up * recenterY(y))).normalize


  def render(scene: Scene, startPos: (Int, Int), endPos: (Int, Int)) {
    val (startX, startY) = startPos
    val (endX, endY) = endPos
    val rayOrigin = scene.camera.position
    for {y <- startY until endY
         x <- startX until endX} {
      val color = (for {offset <- samplingPattern
                        sampleDir = Vec(x, y) + offset
                        rayDir = getPoint(sampleDir.x, sampleDir.y, scene.camera)}
      yield traceRay(Ray(rayOrigin, rayDir), scene, 0)).reduce(_ + _) * 0.25

      setPixel(x, y, color.toScalaFxColor)
    }
  }

}


class ParallelRayTracer(setPixel: (Int, Int, Color) => Unit, scene: Scene, imageSize: (Int, Int), numThreads: Int) {
  val system = ActorSystem("renderer")
  val (width, height) = imageSize
  implicit val timeout = Timeout(15 seconds)
  private val colsPerThread = height / numThreads

  def render() = {
    val rayTracer = new RayTracer(setPixel, imageSize)
    val children = for (i <- 0 until numThreads) yield system.actorOf(Props(classOf[RenderActor], rayTracer, scene))
    val ret = for ((child, idx) <- children.zipWithIndex) yield {
      val startY = colsPerThread * idx
      val endY = colsPerThread * (idx + 1)
      (child ? Render((0, startY), (width, endY))).mapTo[Long]
    }

    //children foreach system.stop
    ret
  }

  def close() {
    system.shutdown()
  }
}

case class Render(startPos: (Int, Int), endPos: (Int, Int))

case class Finished(time: Long)

class RenderActor(rt: RayTracer, scene: Scene) extends Actor {
  def receive = {
    case Render(start, end) => {
      rt.render(scene, start, end)
      sender ! Finished(System.nanoTime())
    }
  }
}