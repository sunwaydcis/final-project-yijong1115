package foodpantry

import foodpantry.database.DatabaseInitializer
import foodpantry.repository.{
  DerbyDistributionAllocationService,
  DerbyDistributionPlanRepository,
  DerbyFoodItemRepository,
  DerbyHouseholdRequestRepository
}
import foodpantry.service.{
  DistributionPlanningService,
  InventoryMonitoringService
}
import foodpantry.ui.{
  DashboardView,
  DistributionPlanView,
  HouseholdRequestView,
  InventoryView
}

import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.Scene
import scalafx.scene.control.{
  Alert,
  Button,
  Label,
  ScrollPane
}
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
        new Alert(Alert.AlertType.Error):
          title = "Database unavailable"
          headerText =
            "The application could not initialise its database."
          contentText =
            "Some features may be unavailable. Details: " +
              exception.getMessage
        .showAndWait()

    val foodItemRepository =
      new DerbyFoodItemRepository

    // ai-assisted: #28
    // why: AI helped connect the persistent household-request feature to navigation.
    val householdRequestRepository =
      new DerbyHouseholdRequestRepository

    // ai-assisted: #39
    // why: AI helped connect the persistent distribution-planning feature to navigation.
    val distributionRepository =
      new DerbyDistributionPlanRepository

    val monitoringService =
      new InventoryMonitoringService

    val distributionPlanningService =
      new DistributionPlanningService

    // ai-assisted: #44
    // why: AI helped route plan creation through the atomic Derby allocation workflow.
    val distributionAllocationService =
      new DerbyDistributionAllocationService(
        distributionPlanningService
      )

    val dashboardPane =
      new DashboardView(
        foodItemRepository,
        monitoringService
      )

    val inventoryPane =
      new InventoryView(
        foodItemRepository,
        distributionRepository
      )

    val requestsPane =
      new HouseholdRequestView(
        householdRequestRepository,
        foodItemRepository
      )

    val distributionPane =
      new DistributionPlanView(
        distributionRepository,
        householdRequestRepository,
        foodItemRepository,
        distributionAllocationService
      )

    val rootPane =
      new BorderPane

    // ai-assisted: #45
    // why: AI helped add consistent scrolling and clear active navigation feedback.
    def scrollable(contentNode: Node): ScrollPane =
      new ScrollPane:
        content = contentNode
        fitToWidth = true
        pannable = true
        styleClass += "content-scroll"

    val dashboardContent =
      scrollable(dashboardPane)

    val inventoryContent =
      scrollable(inventoryPane)

    val requestsContent =
      scrollable(requestsPane)

    val distributionContent =
      scrollable(distributionPane)

    val dashboardButton =
      new Button("Dashboard"):
        maxWidth = Double.MaxValue
        styleClass += "nav-button"

    val inventoryButton =
      new Button("Food Inventory"):
        maxWidth = Double.MaxValue
        styleClass += "nav-button"

    val requestsButton =
      new Button("Household Requests"):
        maxWidth = Double.MaxValue
        styleClass += "nav-button"

    val distributionButton =
      new Button("Distribution Plan"):
        maxWidth = Double.MaxValue
        styleClass += "nav-button"

    val navigationButtons =
      Seq(
        dashboardButton,
        inventoryButton,
        requestsButton,
        distributionButton
      )

    def showContent(
        contentPane: Node,
        selectedButton: Button,
        refreshContent: () => Unit
    ): Unit =
      // ai-assisted: #47
      // why: AI helped refresh each screen whenever navigation opens it.
      refreshContent()
      rootPane.center = contentPane

      navigationButtons.foreach(
        navigationButton =>
          navigationButton.styleClass
            .remove("nav-button-active")
      )

      selectedButton.styleClass +=
        "nav-button-active"

    dashboardButton.onAction = handle {
      showContent(
        dashboardContent,
        dashboardButton,
        () => dashboardPane.refreshView()
      )
    }

    inventoryButton.onAction = handle {
      showContent(
        inventoryContent,
        inventoryButton,
        () => inventoryPane.refreshView()
      )
    }

    requestsButton.onAction = handle {
      showContent(
        requestsContent,
        requestsButton,
        () => requestsPane.refreshView()
      )
    }

    distributionButton.onAction = handle {
      showContent(
        distributionContent,
        distributionButton,
        () => distributionPane.refreshView()
      )
    }

    val navigationPane = new VBox:
      spacing = 10
      padding = Insets(20)
      prefWidth = 210
      styleClass += "sidebar"
      children = Seq(
        new Label("WORKSPACE"):
          styleClass += "sidebar-title",
        dashboardButton,
        inventoryButton,
        requestsButton,
        distributionButton
      )

    val headerPane = new VBox:
      styleClass += "app-header"
      children = Seq(
        new Label(
          "Food Pantry Inventory and Demand Management"
        ):
          styleClass += "app-title",
        new Label(
          "Organise donations, household needs and distributions"
        ):
          styleClass += "app-subtitle"
      )

    rootPane.styleClass += "app-root"
    rootPane.top = headerPane
    rootPane.left = navigationPane
    showContent(
      dashboardContent,
      dashboardButton,
      () => dashboardPane.refreshView()
    )

    val applicationScene =
      new Scene:
        root = rootPane

    Option(getClass.getResource("/styles.css"))
      .foreach: stylesheet =>
        applicationScene.stylesheets +=
          stylesheet.toExternalForm

    stage = new JFXApp3.PrimaryStage:
      title =
        "Food Pantry Inventory and Demand Management System"
      width = 1180
      height = 760
      minWidth = 960
      minHeight = 650
      scene = applicationScene
