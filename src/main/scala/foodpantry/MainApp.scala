package foodpantry

import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{BorderPane, VBox}

object MainApp extends JFXApp3:

  override def start(): Unit =
  // ai-assisted: #5
  // why: AI helped structure four placeholder panes and button navigation using ScalaFX.
    val dashboardPane = new VBox:
      spacing = 10
      padding = Insets(20)
      children = Seq(
        new Label("Dashboard"),
        new Label("Food pantry summary will be displayed here.")
      )

    val inventoryPane = new VBox:
      spacing = 10
      padding = Insets(20)
      children = Seq(
        new Label("Food Inventory"),
        new Label("Donated food records will be managed here.")
      )

    val requestsPane = new VBox:
      spacing = 10
      padding = Insets(20)
      children = Seq(
        new Label("Household Requests"),
        new Label("Household food requests will be managed here.")
      )

    val distributionPane = new VBox:
      spacing = 10
      padding = Insets(20)
      children = Seq(
        new Label("Distribution Plan"),
        new Label("Daily food distribution plans will be prepared here.")
      )

    val rootPane = new BorderPane

    val dashboardButton = new Button("Dashboard"):
      maxWidth = Double.MaxValue
      onAction = handle {
        rootPane.center = dashboardPane
      }

    val inventoryButton = new Button("Food Inventory"):
      maxWidth = Double.MaxValue
      onAction = handle {
        rootPane.center = inventoryPane
      }

    val requestsButton = new Button("Household Requests"):
      maxWidth = Double.MaxValue
      onAction = handle {
        rootPane.center = requestsPane
      }

    val distributionButton = new Button("Distribution Plan"):
      maxWidth = Double.MaxValue
      onAction = handle {
        rootPane.center = distributionPane
      }

    val navigationPane = new VBox:
      spacing = 10
      padding = Insets(20)
      prefWidth = 190
      children = Seq(
        new Label("Navigation"),
        dashboardButton,
        inventoryButton,
        requestsButton,
        distributionButton
      )

    rootPane.left = navigationPane
    rootPane.center = dashboardPane

    stage = new JFXApp3.PrimaryStage:
      title = "Food Pantry Inventory and Demand Management System"
      width = 1000
      height = 650
      scene = new Scene:
        root = rootPane