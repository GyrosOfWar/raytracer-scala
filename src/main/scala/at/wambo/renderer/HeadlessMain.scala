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

  def setPixel(x: Int, y: Int, c: Color) {
    image(x)(y) = c
  }

  val rt = new RayTracer(setPixel, (w, h))
  val scene = Scene(
    things = Vector(
      new Plane(normal = Vec(0, 1, 0), offset = 0) with CheckerboardSurface,
      new Sphere(center = Vec(-1, 0.5, 1.5), radius = 0.5) with DiffuseSurface,
      new Sphere(center = Vec(1, 0.5, 1.5), radius = 0.5) with DiffuseSurface
    ),
    lights = Vector(
      Light(position = Vec(2, 2.5, 0), color = Vec.One) //,
      //Light(position = Vec(1.5, 2.5, 1.5), color = Vec(0.07, 0.07, 0.49)),
      //Light(position = Vec(0, 3.5, 0), color = Vec(0.21, 0.21, 0.35))
    ), camera = Camera(Vec(3, 2, 4), Vec(-3, -1, -1)))

  def main(args: Array[String]) {
    rt.render(scene, (0, 0), (h, w))
    Util.printStats("render")
  }

}
