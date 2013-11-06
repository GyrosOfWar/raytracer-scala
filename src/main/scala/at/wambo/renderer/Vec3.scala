package at.wambo.renderer

import scalafx.scene.paint.Color

/*
 * User: Martin
 * Date: 31.10.13
 * Time: 11:46
 */
case class Vec3(x: Double, y: Double, z: Double) {

  def +(that: Vec3) = Vec3(this.x + that.x, this.y + that.y, this.z + that.z)

  def +(d: Double) = Vec3(this.x + d, this.y + d, this.z + d)

  def unary_-() = Vec3(-this.x, -this.y, -this.z)

  def -(that: Vec3) = this + (-that)

  def *(that: Vec3) = Vec3(this.x * that.x, this.y * that.y, this.z * that.z)

  def *(d: Double) = Vec3(this.x * d, this.y * d, this.z * d)

  def dot(that: Vec3) = this.x * that.x + this.y * that.y + this.z * that.z

  def toScalaFxColor: Color = Color(legalize(x), legalize(y), legalize(z), 1.0)

  def magnitude = math.sqrt(this.dot(this))

  def normalize: Vec3 = {
    val mag = magnitude
    this * (if (mag == 0) Double.PositiveInfinity else 1.0 / mag)
  }

  def cross(that: Vec3) = Vec3(
    this.y * that.z - this.z * that.y,
    this.z * that.x - this.x * that.z,
    this.x * that.y - this.y * that.x)

  @inline private def legalize(d: Double) = if (d > 1.0) 1.0 else d
}

object Vec3 {
  val Zero = Vec3(0, 0, 0)
  val One = Vec3(1, 1, 1)
}

case class Vec2(x: Double, y: Double) {
  def +(that: Vec2) = Vec2(this.x + that.x, this.y + that.y)

  def unary_- = Vec2(-this.x, -this.y)

  def -(that: Vec2) = this + (-that)

  def *(that: Vec2) = Vec2(this.x * that.x, this.y * that.y)

  def +(d: Double) = Vec2(this.x + d, this.y + d)
  
  def *(d: Double) = Vec2(this.x * d, this.y * d)
}

object Vec2 {
  val Zero = Vec2(0, 0)
  val One = Vec2(1, 1)
}

case class Ray(start: Vec3, direction: Vec3)

case class Intersection(thing: SceneObject, ray: Ray, distance: Double)

case class Surface(diffuse: Vec3 => Vec3, specular: Vec3 => Vec3, reflect: Vec3 => Double, roughness: Double)

object Surface {
  /**
   * ShinySurface. Also see [[at.wambo.renderer.ShinySurface]]
   */
  val Default = Surface(
    diffuse = pos => Vec3(1, 1, 1),
    specular = pos => Vec3(0.5, 0.5, 0.5),
    reflect = pos => 0.6,
    roughness = 50
  )
}

case class Light(position: Vec3, color: Vec3)

case class Camera(position: Vec3, lookAt: Vec3) {
  val forward = (lookAt - position).normalize
  val down = Vec3(0, -1, 0)
  val right = forward.cross(down).normalize * 1.5
  val up = forward.cross(right).normalize * 1.5
}

case class Scene(things: Vector[SceneObject], lights: Vector[Light], camera: Camera) {
  def intersect(ray: Ray) = things.map(_.intersect(ray))
}

object Util {
  private var times = Map.empty[String, Seq[Long]].withDefaultValue(List.empty[Long])

  def timedCall[A](functionName: String, printTime: Boolean = true)(block: => A): A = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()

    if (printTime)
      println(s"$functionName: Time: ${BigDecimal(t1 - t0) / BigDecimal(1000000)} ms")
    times = times + (functionName -> (times(functionName) :+ (t1 - t0)))
    result
  }

  def printStats(functionName: String) {
    require(times(functionName).nonEmpty)

    val fnTimes = (for {
      (name, ts) <- times
      time <- ts
      if name == functionName
    } yield BigDecimal(time) / BigDecimal(1000000)).toSeq

    val median = fnTimes.sorted.apply(fnTimes.length / 2)
    println(s"Showing profiling information for: $functionName")
    println(s"Median time: $median ms")
    println(s"Max/Min/Range: ${fnTimes.max} ms / ${fnTimes.min} ms / ${fnTimes.max - fnTimes.min} ms")
    println(s"Number of samples: ${fnTimes.length}")
  }

}