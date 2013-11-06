package at.wambo.renderer

/*
 * User: Martin
 * Date: 31.10.13
 * Time: 11:46
 */
sealed trait SceneObject {
  def intersect(ray: Ray): Option[Intersection]

  def normal(pos: Vec3): Vec3

  val surface: Surface
}

sealed trait HasSurface {
  // ShinySurface is default
  val surface: Surface = Surface.Default
}

trait ShinySurface extends HasSurface {
  override val surface = Surface(
    diffuse = pos => Vec3(1, 1, 1),
    specular = pos => Vec3(0.5, 0.5, 0.5),
    reflect = pos => 0.6,
    roughness = 50
  )
}

trait CheckerboardSurface extends HasSurface {
  override val surface = Surface(
    diffuse = pos => {
      if ((math.floor(pos.z).toInt + math.floor(pos.x).toInt) % 2 != 0) {
        Vec3(0.7, 0.14, 0.07)
      }
      else {
        Vec3(0.1, 0.2, 0.7)
      }
    },
    specular = pos => Vec3(1, 1, 1),
    reflect = pos => {
      if ((math.floor(pos.z).toInt + math.floor(pos.x).toInt) % 2 != 0)
        0.2
      else
        0.9
    },
    roughness = 200
  )
}

trait TestSurface extends HasSurface {
  override val surface = Surface(
    diffuse = pos => Vec3(1, 1, 1),
    specular = pos => Vec3(0.5, 0.5, 0.5),
    reflect = pos => 0.9,
    roughness = 50
  )
}

trait DiffuseSurface extends HasSurface {
  override val surface = Surface(
    diffuse = pos => Vec3(1, 1, 1),
    specular = pos => Vec3(0, 0, 0),
    reflect = pos => 0,
    roughness = 0
  )
}

case class Sphere(center: Vec3, radius: Double) extends SceneObject with HasSurface {
  override def intersect(ray: Ray) = {
    val eo = center - ray.start
    val v = eo.dot(ray.direction)
    val dist = {
      if (v < 0) {
        0
      }
      else {
        val disc = (radius * radius) - (eo.dot(eo) - (v * v))
        if (disc < 0)
          0
        else
          v - math.sqrt(disc)
      }
    }
    if (dist == 0)
      None
    else
      Some(Intersection(thing = this, ray = ray, distance = dist))

  }

  def normal(pos: Vec3): Vec3 = (pos - center).normalize
}

case class Plane(normal: Vec3, offset: Double) extends SceneObject with HasSurface {
  def intersect(ray: Ray): Option[Intersection] = {
    val denominator = normal.dot(ray.direction)
    if (denominator > 0)
      None
    else {
      Some(Intersection(
        thing = this,
        ray = ray,
        distance = (normal.dot(ray.start) + offset) / -denominator
      ))
    }
  }

  def normal(pos: Vec3): Vec3 = normal
}