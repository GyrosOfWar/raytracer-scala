package at.wambo.renderer

import scalafx.scene.paint.Color
import concurrent.Future

/*
 * User: Martin
 * Date: 08.11.13
 * Time: 15:58
 */
class PathTracer(val imageWidth: Int, val imageHeight: Int) extends Renderer {
  def render(scene: Scene): Future[Array[Color]] = ???
}
