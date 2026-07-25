package foodpantry.ui

import foodpantry.model.FoodItem
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, Label, TableColumn, TableView}
import scalafx.scene.layout.VBox

import scala.util.{Failure, Success}

// ai-assisted: #14
// why: AI helped build a ScalaFX inventory table backed by the repository contract.
class InventoryView(
    private val repository: Repository[FoodItem]
) extends VBox:

  private val foodItems =
    ObservableBuffer.empty[FoodItem]

  private val statusLabel =
    new Label("Inventory has not been loaded.")

  private val inventoryTable =
    new TableView[FoodItem](foodItems):
      prefHeight = 450
      columns ++= List(
        textColumn("ID", foodItem => foodItem.id),
        textColumn("Name", foodItem => foodItem.name),
        textColumn(
          "Category",
          foodItem => foodItem.category
        ),
        textColumn(
          "Quantity",
          foodItem => foodItem.quantity.toString
        ),
        textColumn("Unit", foodItem => foodItem.unit),
        textColumn(
          "Expiry Date",
          foodItem =>
            foodItem.expiryDate
              .map(_.toString)
              .getOrElse("-")
        )
      )
  private val foodItemForm =
    new FoodItemForm(
      repository,
      () => loadItems()
    )
  private val refreshButton =
    new Button("Refresh Inventory"):
      onAction = handle {
        loadItems()
      }
  // ai-assisted: #16
  // why: AI helped add safe deletion for the currently selected inventory record.
  private val deleteButton =
    new Button("Delete Selected"):
      onAction = handle {
        deleteSelectedItem()
      }
  spacing = 10
  padding = Insets(20)
  children = Seq(
    new Label("Food Inventory"),
    foodItemForm,
    refreshButton,
    deleteButton,
    inventoryTable,
    statusLabel
  )

  loadItems()
  
  private def deleteSelectedItem(): Unit =
    Option(
      inventoryTable.selectionModel().selectedItem.value
    ) match
      case Some(foodItem) =>
        repository.delete(foodItem.id) match
          case Success(true) =>
            statusLabel.text =
              s"${foodItem.name} was deleted successfully."
            loadItems()

          case Success(false) =>
            statusLabel.text =
              "The selected food item was not found."

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to delete item: ${exception.getMessage}"

      case None =>
        statusLabel.text =
          "Select a food item before deleting."

  private def textColumn(
      heading: String,
      extractValue: FoodItem => String
  ): TableColumn[FoodItem, String] =
    new TableColumn[FoodItem, String]:
      text = heading
      cellValueFactory = cellData =>
        StringProperty(
          extractValue(cellData.value)
        )

  private def loadItems(): Unit =
    repository.findAll() match
      case Success(items) =>
        foodItems.clear()
        foodItems ++= items
        statusLabel.text =
          s"${items.size} food item(s) loaded."

      case Failure(exception) =>
        statusLabel.text =
          s"Unable to load inventory: ${exception.getMessage}"