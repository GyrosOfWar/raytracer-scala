package at.wambo.renderer

/*
 * User: Martin
 * Date: 31.10.13
 * Time: 11:46
 */
sealed trait SceneObject {
  def intersect(ray: Ray): Option[Intersection]

  def normal(pos: Vec): Vec

  val surface: Surface
}

sealed trait HasSurface {
  // ShinySurface is default
  val surface: Surface = Surface.Default
}

trait ShinySurface extends HasSurface {
  override val surface = Surface(
    diffuse = pos => Vec(1, 1, 1),
    specular = pos => Vec(0.5, 0.5, 0.5),
    reflect = pos => .6,
    roughness = 50
  )
}

trait CheckerboardSurface extends HasSurface {
  override val surface = Surface(
    diffuse = pos => {
      if ((math.floor(pos.z).toInt + math.floor(pos.x).toInt) % 2 != 0) {
        Vec(1, 1, 1)
      }
      else {
        Vec(0, 0, 0)
      }
    },
    specular = pos => Vec(1, 1, 1),
    reflect = pos => {
      if ((math.floor(pos.z).toInt + math.floor(pos.x).toInt) % 2 != 0)
        0.1
      else
        0.7
    },
    roughness = 200
  )
}

case class Sphere(center: Vec, radius: Double) extends SceneObject with HasSurface {
  override def intersect(ray: Ray) = {
    val eo = center - ray.start
    val v = eo.dot(ray.direction)
    val dist = {
      if (v < 0) {
        0
      }
      else {
        val disc = math.pow(radius, 2) - (eo.dot(eo) - math.pow(v, 2))
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

  def normal(pos: Vec): Vec = (pos - center).normalize
}

case class Plane(normal: Vec, offset: Double) extends SceneObject with HasSurface {
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

  def normal(pos: Vec): Vec = normal
}