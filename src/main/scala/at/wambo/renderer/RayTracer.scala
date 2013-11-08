package at.wambo.renderer

import scalafx.scene.paint.Color
import scala.collection.mutable.ArrayBuffer
import concurrent.Future
import concurrent.future
import concurrent.ExecutionContext.Implicits.global

/*
 * User: Martin
 * Date: 30.10.13
 * Time: 23:40
 */

trait Renderer {
  val screenHeight: Int
  val screenWidth: Int

  def render(scene: Scene): Future[Array[Color]]
}

class RayTracer(val screenWidth: Int, val screenHeight: Int, AAEnabled: Boolean = true) extends Renderer {
  private val maxDepth = 5
  private val backgroundColor = Vec3.Zero
  private val defaultColor = Vec3.Zero
  private val samplingPattern = List(Vec2(0, 0.25), Vec2(0, -0.25), Vec2(0.25, 0), Vec2(-0.25, 0), Vec2(0, 0)).map(_ * 2)
  //private val samplingPattern = for(_ <- 1 to 4) yield Vec2(math.random-1, math.random-1)

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
  private def getNaturalColor(thing: SceneObject, pos: Vec3, norm: Vec3, reflectDir: Vec3, scene: Scene): Vec3 = {
    (for (light <- scene.lights) yield {
      val lightDir = light.position - pos
      val lightVec = lightDir.normalize
      val is = testRay(Ray(start = pos, direction = lightVec), scene)
      val isInShadow = !((is > lightDir.magnitude) || is == 0)
      if (!isInShadow) {
        val illum = lightVec.dot(norm)
        val lightColor = if (illum > 0) light.color * illum else Vec3.Zero
        val specular = lightVec.dot(reflectDir.normalize)
        val specularColor = if (specular > 0) light.color * math.pow(specular, thing.surface.roughness) else Vec3.Zero
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
  private def getReflectionColor(thing: SceneObject, pos: Vec3, norm: Vec3, reflectDir: Vec3, scene: Scene, depth: Int): Vec3 = {
    traceRay(Ray(pos, reflectDir), scene, depth + 1) * thing.surface.reflect(pos)
  }

  /**
   * Does the complete shading for one intersection, with reflections and normal shading.
   * @param intersection The intersection, consisting of the SceneObject, the ray and the distance.
   * @param scene Scene to use.
   * @param depth Current recursion depth.
   * @return Color of the point at which the intersection occurred.
   */
  private def shade(intersection: Intersection, scene: Scene, depth: Int): Vec3 = {
    val d = intersection.ray.direction
    val pos = d * intersection.distance + intersection.ray.start
    val normal = intersection.thing.normal(pos)
    val reflectDir = d - ((normal * normal.dot(d)) * 2)
    val ret = defaultColor + getNaturalColor(intersection.thing, pos, normal, reflectDir, scene)
    if (depth >= maxDepth)
      ret + Vec3(0.5, 0.5, 0.5)
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
  private def traceRay(ray: Ray, scene: Scene, depth: Int): Vec3 =
    intersections(ray, scene).headOption match {
      case Some(intersection) => shade(intersection, scene, depth)
      case None => backgroundColor
    }

  private def recenterX(x: Double): Double = (x - (screenWidth / 2.0)) / (2.0 * screenWidth)

  private def recenterY(y: Double): Double = -(y - (screenHeight / 2.0)) / (2.0 * screenHeight)

  private def getPoint(pos: Vec2, camera: Camera): Vec3 =
    (camera.forward + (camera.right * recenterX(pos.x) + camera.up * recenterY(pos.y))).normalize

  def render(scene: Scene) = future {
    render(scene, (0, 0), (screenWidth, screenHeight))
  }

  def render(scene: Scene, startPos: (Int, Int), endPos: (Int, Int)): Array[Color] = {
    val (startX, startY) = startPos
    val (endX, endY) = endPos
    val rayOrigin = scene.camera.position
    val sampleCount = samplingPattern.length
    val invSampleCount = 1.0 / sampleCount
    val colors = ArrayBuffer.empty[Color]
    for {y <- startY until endY
         x <- startX until endX} {
      val color = {
        if (AAEnabled) {
          (for {offset <- samplingPattern
                sampleDir = Vec2(x, y) + offset
                rayDir = getPoint(sampleDir, scene.camera)}
          yield traceRay(Ray(rayOrigin, rayDir), scene, 0)).reduce(_ + _) * invSampleCount
        } else {
          traceRay(Ray(rayOrigin, getPoint(Vec2(x, y), scene.camera)), scene, 0)
        }
      }


      colors += color.toScalaFxColor
    }
    colors.toArray
  }
}