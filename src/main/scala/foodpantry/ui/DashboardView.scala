package foodpantry.ui

import foodpantry.model.FoodItem
import foodpantry.repository.Repository
import foodpantry.service.InventoryMonitoringService

import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, Label, ListView}
import scalafx.scene.layout.VBox

import scala.util.{Failure, Success}

// ai-assisted: #20
// why: AI helped connect inventory monitoring results to a ScalaFX dashboard.
class DashboardView(
    private val repository: Repository[FoodItem],
    private val monitoringService: InventoryMonitoringService
) extends VBox:

  private val totalItemsLabel =
    new Label("Food records: 0")

  private val totalQuantityLabel =
    new Label("Total quantity: 0")

  private val alertMessages =
    ObservableBuffer.empty[String]

  private val alertList =
    new ListView[String](alertMessages):
      prefHeight = 350

  private val statusLabel =
    new Label("Dashboard has not been loaded.")

  private val refreshButton =
    new Button("Refresh Dashboard"):
      onAction = handle {
        refreshDashboard()
      }

  spacing = 10
  padding = Insets(20)

  children = Seq(
    new Label("Food Pantry Dashboard"),
    totalItemsLabel,
    totalQuantityLabel,
    new Label("Inventory Alerts"),
    refreshButton,
    alertList,
    statusLabel
  )

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