import at.wambo.renderer._
import concurrent.Await
import scalafx.application.JFXApp
import scalafx.event.ActionEvent
import scalafx.scene.control.Button
import scalafx.scene.image.{ImageView, WritableImage}
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import concurrent.duration._
import scalafx.event.EventIncludes._

object Main extends JFXApp {
  val w = 800
  val h = 600
  val numOfThreads = 4
  val rendererImage = new WritableImage(w, h)
  val writer = rendererImage.pixelWrit

  def setPixel(x: Int, y: Int, color: Color) {
    writer.setColor(x, y, color)
  }

  val rendererScene = Scene(
    things = Vector(
      new Plane(normal = Vec(0, 1, 0), offset = 0) with CheckerboardSurface,
      new Sphere(center = Vec(-1, -1, 0), radius = 1) with ShinySurface,
      new Sphere(center = Vec(-1, 0.5, 1.5), radius = 0.5) with ShinySurface
    ),
    lights = Vector(
      Light(position = Vec(-2, 2.5, 0), color = Vec.One),
      Light(position = Vec(1.5, 2.5, 1.5), color = Vec(0.07, 0.07, 0.49)),
      Light(position = Vec(0, 3.5, 0), color = Vec(0.21, 0.21, 0.35))
    ), camera = Camera(Vec(3, 2, 4), Vec(-3, -1, -1)))

  val parRt = new ParallelRayTracer(writer, rendererScene, (w, h), numOfThreads)

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
    for(f <- futures) {
      Await.ready(f, 3 minutes)
    }
    parRt.close()

    Util.printStats("render")
  }

  stage = new JFXApp.PrimaryStage {
    title = "RayTracer"
    height = h + renderButton.prefHeight() + 10
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