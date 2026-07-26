package foodpantry

import foodpantry.database.DatabaseInitializer
import foodpantry.repository.DerbyFoodItemRepository
import foodpantry.service.InventoryMonitoringService
import foodpantry.ui.{DashboardView, InventoryView}

import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{BorderPane, VBox}

import scala.util.{Failure, Success}

object MainApp extends JFXApp3:

  override def start(): Unit =
    // ai-assisted: #10
    // why: AI suggested testing schema initialization from the normal forked application.
    DatabaseInitializer.initialize() match
      case Success(_) =>
        println("Database initialized successfully.")

      case Failure(exception) =>
        println(
          s"Database initialization failed: ${exception.getMessage}"
        )

    val foodItemRepository =
      new DerbyFoodItemRepository

    val monitoringService =
      new InventoryMonitoringService

    val dashboardPane =
      new DashboardView(
        foodItemRepository,
        monitoringService
      )

    val inventoryPane =
      new InventoryView(foodItemRepository)

    val requestsPane = new VBox:
      spacing = 10
      padding = Insets(20)
      children = Seq(
        new Label("Household Requests"),
        new Label(
          "Household food requests will be managed here."
        )
      )

    val distributionPane = new VBox:
      spacing = 10
      padding = Insets(20)
      children = Seq(
        new Label("Distribution Plan"),
        new Label(
          "Daily food distribution plans will be prepared here."
        )
      )

    val rootPane =
      new BorderPane

    val dashboardButton =
      new Button("Dashboard"):
        maxWidth = Double.MaxValue
        onAction = handle {
          rootPane.center = dashboardPane
        }

    val inventoryButton =
      new Button("Food Inventory"):
        maxWidth = Double.MaxValue
        onAction = handle {
          rootPane.center = inventoryPane
        }

    val requestsButton =
      new Button("Household Requests"):
        maxWidth = Double.MaxValue
        onAction = handle {
          rootPane.center = requestsPane
        }

    val distributionButton =
      new Button("Distribution Plan"):
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
      title =
        "Food Pantry Inventory and Demand Management System"
      width = 1000
      height = 650
      scene = new Scene:
        root = rootPane