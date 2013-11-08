import at.wambo.renderer._
import scalafx.application.{Platform, JFXApp}
import scalafx.event.ActionEvent
import scalafx.scene.control.Button
import scalafx.scene.image.{ImageView, WritableImage}
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import scalafx.event.EventIncludes._

object Main extends JFXApp {
  val w = 800
  val h = 600
  val numOfThreads = 4
  val rendererImage = new WritableImage(w, h)
  val writer = rendererImage.pixelWrit

  def setPixel(x: Int, y: Int, color: Color) {
    Platform.runLater {
      imageView.image.update(rendererImage)
      writer.setColor(x, y, color)
    }
  }

  val rendererScene = Scene(
    things = Vector(
      new Plane(normal = Vec3(0, 1, 0), offset = 0) with ShinySurface,
      new Sphere(center = Vec3(-1, 0.5, 1.5), radius = 0.5) with ShinySurface,
      new Sphere(center = Vec3(1, 0.5, 1.5), radius = 0.5) with DiffuseSurface
    ),
    lights = Vector(
      Light(position = Vec3(2, 1, 3), color = Vec3.One),
      Light(position = Vec3(-4, 5, 3), color = Vec3.One)
    ), camera = Camera(Vec3(3, 2, 4), Vec3(-3, -1, -1)))

  val parRt = new ParallelRayTracer(setPixel, w, h, numOfThreads)

  lazy val imageView = new ImageView()

  lazy val renderButton = new Button("Render") {
    prefHeight = 25
    onAction = (e: ActionEvent) => {
      render()
    }
  }

  def render() {
    parRt.render(rendererScene)
  }

  stage = new JFXApp.PrimaryStage {
    title = "RayTracer"
    height = h
    width = w
    resizable = false

    onCloseRequest = {
      parRt.close()
    }

    scene = new scalafx.scene.Scene(w, h) {
      root = new VBox {
        spacing = 5
        content = List(imageView, renderButton)
      }
    }
  }
}