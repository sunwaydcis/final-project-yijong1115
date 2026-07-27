package foodpantry.ui

import foodpantry.model.FoodItem
import foodpantry.repository.Repository
import foodpantry.service.InventoryMonitoringService

import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{Label, ListView}
import scalafx.scene.layout.{FlowPane, Priority, VBox}

import scala.util.{Failure, Success}

// ai-assisted: #20
// why: AI helped connect inventory monitoring results to a ScalaFX dashboard.
class DashboardView(
    private val repository: Repository[FoodItem],
    private val monitoringService: InventoryMonitoringService
) extends VBox:

  private val totalItemsLabel =
    new Label("Food records: 0"):
      styleClass += "summary-card"

  private val totalQuantityLabel =
    new Label("Total quantity: 0"):
      styleClass += "summary-card"

  private val summaryPane =
    new FlowPane:
      hgap = 12
      vgap = 10
      children = Seq(
        totalItemsLabel,
        totalQuantityLabel
      )

  private val alertMessages =
    ObservableBuffer.empty[String]

  private val alertList =
    new ListView[String](alertMessages):
      prefHeight = 380
      placeholder =
        new Label("No inventory alerts to display.")

  private val statusLabel =
    UiComponents.statusLabel(
      "Dashboard has not been loaded."
    )

  private val refreshButton =
    UiComponents.secondaryButton("Refresh Dashboard")

  refreshButton.onAction = handle {
    refreshDashboard()
  }

  // ai-assisted: #45
  // why: AI helped reorganise the dashboard into clear summary and alert sections.
  spacing = 14
  padding = Insets(24)
  styleClass += "page"

  VBox.setVgrow(alertList, Priority.Always)

  children = Seq(
    UiComponents.pageTitle("Dashboard"),
    UiComponents.pageDescription(
      "Review current stock totals and items that need attention."
    ),
    summaryPane,
    UiComponents.sectionTitle("Inventory alerts"),
    refreshButton,
    alertList,
    statusLabel
  )

  def refreshView(): Unit =
    refreshDashboard()

  private def refreshDashboard(): Unit =
    repository.findAll() match
      case Success(foodItems) =>
        val alerts =
          monitoringService.findAlerts(foodItems)

        val totalQuantity =
          foodItems
            .map(foodItem => foodItem.quantity)
            .sum

        totalItemsLabel.text =
          s"Food records: ${foodItems.size}"

        totalQuantityLabel.text =
          s"Total quantity: $totalQuantity"

        alertMessages.clear()

        if alerts.isEmpty then
          alertMessages +=
            "No low-stock or expiry alerts."
        else
          alertMessages ++=
            alerts.map(
              inventoryAlert => inventoryAlert.message
            )

        statusLabel.text =
          s"${alerts.size} inventory alert(s) found."

      case Failure(exception) =>
        alertMessages.clear()
        statusLabel.text =
          s"Unable to load dashboard: ${exception.getMessage}"
