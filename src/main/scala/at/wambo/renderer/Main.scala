import at.wambo.renderer._
import scalafx.application.JFXApp
import scalafx.event.ActionEvent
import scalafx.scene.control.{ProgressBar, Button}
import scalafx.scene.image.{PixelFormat, ImageView, WritableImage}
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import scalafx.event.EventIncludes._

object Main extends JFXApp {
  val w = 800
  val h = 600
  val numOfThreads = 4
  val rendererImage = new WritableImage(w, h)
  val writer = rendererImage.pixelWrit

  def colorToByteArray(arr: Array[Color]): Array[Byte] = {
    val imageData = Array.fill[Byte](arr.size * 3)(0)
    var k = 0
    for (c <- arr) {
      val r = c.red * 255.0
      val g = c.green * 255.0
      val b = c.blue * 255.0
      imageData(k) = r.toByte
      imageData(k + 1) = g.toByte
      imageData(k + 2) = b.toByte

      k += 3
    }
    imageData
  }

  val rendererScene = Scene(
    things = Vector(
      new Plane(normal = Vec3(0, 1, 0), offset = 0) with CheckerboardSurface,
      new Sphere(center = Vec3(-1, 0.5, 1.5), radius = 0.5) with ShinySurface,
      new Sphere(center = Vec3(1, 0.5, 1.5), radius = 0.5) with DiffuseSurface
    ),
    lights = Vector(
      Light(position = Vec3(2, 1, 3), color = Vec3.One),
      Light(position = Vec3(-4, 5, 3), color = Vec3.One)
    ), camera = Camera(Vec3(3, 2, 4), Vec3(-3, -1, -1)))

   val parRt = new ParallelRayTracer(w, h, numOfThreads)
  //val rt = new RayTracer(w, h, true)
  lazy val imageView = new ImageView {
    visible = false
  }

  lazy val renderButton = new Button("Render") {
    onAction = (_: ActionEvent) => render()
  }

  lazy val progressBar = new ProgressBar {
    prefWidth = 200
    prefHeight = 15
    visible = false
    //progress <== parRt.pixelsDrawn / (h * w).toDouble
  }

  def render() {
     import parRt.system.dispatcher
    //import concurrent.ExecutionContext.Implicits.global

    val renderResult = parRt.render(rendererScene)
    imageView.visible = false
    renderButton.visible = false
    progressBar.visible = true

    renderResult onSuccess {
      case result =>
        val pixels = colorToByteArray(result)
        writer.setPixels(0, 0, w, h, PixelFormat.getByteRgbInstance, pixels, 0, w * 3)
        imageView.visible = true
        renderButton.visible = false
        progressBar.visible = false
        imageView.image() = rendererImage
    }
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
        content = List(imageView, progressBar, renderButton)
      }
    }
  }
}