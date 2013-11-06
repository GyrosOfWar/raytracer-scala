package at.wambo.renderer

import scalafx.scene.paint.Color
import scala.concurrent.Await
import scala.concurrent.duration._

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
      new Plane(normal = Vec(0, 1, 0), offset = 0) with CheckerboardSurface,
      new Sphere(center = Vec(-1, 0.5, 1.5), radius = 0.5) with DiffuseSurface,
      new Sphere(center = Vec(1, 0.5, 1.5), radius = 0.5) with DiffuseSurface
    ),
    lights = Vector(
      Light(position = Vec(2, 2.5, 0), color = Vec.One)
    ), camera = Camera(Vec(3, 2, 4), Vec(-3, -1, -1)))


  val prt = new ParallelRayTracer((x: Int, y: Int, c: Color) => image(x)(y) = c, scene, (h, w), 4)

  def main(args: Array[String]) {
    val xs = prt.render()
    for (f <- xs) yield {
      Await.ready(f, 2 minutes)
    }
    Util.printStats("render")
    prt.close()
  }

}
