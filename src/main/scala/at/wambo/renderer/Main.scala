import at.wambo.renderer._
import scala.concurrent.Await
import scala.concurrent.duration._
import scalafx.application.JFXApp
import scalafx.event.ActionEvent
import scalafx.scene.control.Button
import scalafx.scene.image.{ImageView, WritableImage}
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import scalafx.event.EventIncludes._

object Main extends JFXApp {
  val w = 800
  val h = 600
  val numOfThreads = 8
  val rendererImage = new WritableImage(w, h)
  val writer = rendererImage.pixelWrit

  def setPixel(x: Int, y: Int, color: Color) {
    writer.setColor(x, y, color)
  }

  val rendererScene = Scene(
    things = Vector(
      new Plane(normal = Vec(0, 1, 0), offset = 0) with CheckerboardSurface,
      new Sphere(center = Vec(-1, 0.5, 1.5), radius = 0.5) with DiffuseSurface,
      new Sphere(center = Vec(1, 0.5, 1.5), radius = 0.5) with DiffuseSurface
    ),
    lights = Vector(
      Light(position = Vec(2, 2.5, 0), color = Vec.One)
    ), camera = Camera(Vec(3, 2, 4), Vec(-3, -1, -1)))

  val parRt = new ParallelRayTracer(setPixel, rendererScene, (w, h), numOfThreads)

  lazy val imageView = new ImageView()

  lazy val renderButton = new Button("Render") {
    prefHeight = 25
    onAction = (e: ActionEvent) => {
      render()
      imageView.image = rendererImage
    }
  }

  def render() {
    val futures = parRt.render()
    for (f <- futures) {
      Await.ready(f, 3 minutes)
    }
    parRt.close()
    //Util.printStats("render")
  }

  stage = new JFXApp.PrimaryStage {
    title = "RayTracer"
    height = h
    width = w
    resizable = false
    scene = new scalafx.scene.Scene(w, h) {
      root = new VBox {
        spacing = 5
        content = List(imageView, renderButton)
      }
    }
  }
}