package at.wambo.renderer

import scalafx.scene.paint.Color

/*
 * User: Martin
 * Date: 05.11.13
 * Time: 23:32
 */
object HeadlessMain {
  val w = 800
  val h = 600
  val image = Array.fill(h, w)(Color(0, 0, 0, 1))

  val scene = Scene(
    things = Vector(
      new Plane(normal = Vec3(0, 1, 0), offset = 0) with CheckerboardSurface,
      new Sphere(center = Vec3(-1, 0.5, 1.5), radius = 0.5) with DiffuseSurface,
      new Sphere(center = Vec3(1, 0.5, 1.5), radius = 0.5) with DiffuseSurface
    ),
    lights = Vector(
      Light(position = Vec3(2, 2.5, 0), color = Vec3.One)
    ), camera = Camera(Vec3(3, 2, 4), Vec3(-3, -1, -1)))


  val prt = new ParallelRayTracer((x: Int, y: Int, c: Color) => image(x)(y) = c, h, w, 4)

  def main(args: Array[String]) {
    prt.render(scene)
    prt.close()
  }

}
