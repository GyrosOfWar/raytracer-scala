package at.wambo.renderer

import scalafx.scene.paint.Color

/*
 * User: Martin
 * Date: 31.10.13
 * Time: 11:46
 */
case class Vec(x: Double, y: Double, z: Double) {
  def +(that: Vec) = Vec(this.x + that.x, this.y + that.y, this.z + that.z)

  def +(d: Double) = Vec(this.x + d, this.y + d, this.z + d)

  def unary_-() = Vec(-this.x, -this.y, -this.z)

  def -(that: Vec) = this + (-that)

  def *(that: Vec) = Vec(this.x * that.x, this.y * that.y, this.z * that.z)

  def *(d: Double) = Vec(this.x * d, this.y * d, this.z * d)

  def dot(that: Vec) = this.x * that.x + this.y * that.y + this.z * that.z

  def toScalaFxColor: Color = Color(legalize(x), legalize(y), legalize(z), 1.0)

  def magnitude = math.sqrt(this.dot(this))

  def normalize: Vec = {
    val mag = magnitude
    this * (if (mag == 0) Double.PositiveInfinity else 1.0 / mag)
  }

  def cross(that: Vec) = Vec(
    this.y * that.z - this.z * that.y,
    this.z * that.x - this.x * that.z,
    this.x * that.y - this.y * that.x)

  @inline private def legalize(d: Double) = if (d > 1.0) 1.0 else d

}

object Vec {
  val Zero = Vec(0, 0, 0)
  val One = Vec(1, 1, 1)
}

case class Ray(start: Vec, direction: Vec)

case class Intersection(thing: SceneObject, ray: Ray, distance: Double)

case class Surface(diffuse: Vec => Vec, specular: Vec => Vec, reflect: Vec => Double, roughness: Double)

object Surface {
  val Default = Surface(
    diffuse = pos => Vec(1, 1, 1),
    specular = pos => Vec(0.5, 0.5, 0.5),
    reflect = pos => .6,
    roughness = 50
  )
}

case class Light(position: Vec, color: Vec)

case class Camera(position: Vec, lookAt: Vec) {
  val forward = (lookAt - position).normalize
  val down = Vec(0, -1, 0)
  val right = forward.cross(down).normalize * 1.5
  val up = forward.cross(right).normalize * 1.5
}

case class Scene(things: Vector[SceneObject], lights: Vector[Light], camera: Camera) {
  def intersect(ray: Ray) = things.map(_.intersect(ray))
}

object Util {
  def timedCall[A](msg: String)(block: => A): A = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println(s"$msg, Time: ${(t1 - t0) / 1000000.toDouble} ms")
    result
  }

}