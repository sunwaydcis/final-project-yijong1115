package foodpantry

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane

// ai-assisted: #4
// why: AI was used to check the correct ScalaFX JFXApp3 application structure.
object MainApp extends JFXApp3:

  override def start(): Unit =
    stage = new JFXApp3.PrimaryStage:
      title = "Food Pantry Inventory and Demand Management System"
      width = 1000
      height = 650
      scene = new Scene:
        root = new BorderPane:
          center = new Label("Food Pantry Inventory and Demand Management System")