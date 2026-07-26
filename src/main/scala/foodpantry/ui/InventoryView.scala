package foodpantry.ui

import foodpantry.model.{
  FoodItem,
  PerishableFood,
  ShelfStableFood
}
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  Alert,
  ButtonType,
  Label,
  TableColumn,
  TableView,
  TextField
}
import scalafx.scene.layout.{FlowPane, Priority, VBox}

import scala.util.{Failure, Success, Try}

// ai-assisted: #14
// why: AI helped build a ScalaFX inventory table backed by the repository contract.
class InventoryView(
    private val repository: Repository[FoodItem]
) extends VBox:

  private val foodItems =
    ObservableBuffer.empty[FoodItem]

  private val statusLabel =
    UiComponents.statusLabel(
      "Inventory has not been loaded."
    )

  private val inventoryTable =
    new TableView[FoodItem](foodItems):
      prefHeight = 390
      placeholder =
        new Label(
          "No food items yet. Use the form above to add a donation."
        )
      columns ++= List(
        textColumn("Name", foodItem => foodItem.name),
        textColumn(
          "Category",
          foodItem => foodItem.category
        ),
        textColumn(
          "Quantity",
          foodItem => foodItem.quantity.toString
        ),
        textColumn(
          "Unit",
          foodItem => foodItem.unit
        ),
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
    UiComponents.secondaryButton("Refresh")

  refreshButton.onAction = handle {
    loadItems()
  }

  // ai-assisted: #16
  // why: AI helped add safe deletion for the currently selected inventory record.
  private val deleteButton =
    UiComponents.dangerButton("Delete Selected")

  deleteButton.onAction = handle {
    deleteSelectedItem()
  }

  private val newQuantityField =
    new TextField:
      promptText = "New quantity"
      prefWidth = 140

  // ai-assisted: #17
  // why: AI helped add safe quantity updates for the selected immutable food item.
  private val updateQuantityButton =
    UiComponents.primaryButton("Update Quantity")

  updateQuantityButton.onAction = handle {
    updateSelectedQuantity()
  }

  private val inventoryActions =
    new FlowPane:
      hgap = 10
      vgap = 10
      styleClass += "action-bar"
      children = Seq(
        refreshButton,
        newQuantityField,
        updateQuantityButton,
        deleteButton
      )

  // ai-assisted: #45
  // why: AI helped group inventory tasks and add clearer hierarchy and feedback.
  spacing = 14
  padding = Insets(24)
  styleClass += "page"

  VBox.setVgrow(inventoryTable, Priority.Always)

  children = Seq(
    UiComponents.pageTitle("Food Inventory"),
    UiComponents.pageDescription(
      "Record donations, adjust quantities and review expiry information."
    ),
    UiComponents.formSection(
      "Add donated food",
      foodItemForm
    ),
    UiComponents.sectionTitle("Current inventory"),
    inventoryActions,
    inventoryTable,
    statusLabel
  )

  loadItems()

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

  private def deleteSelectedItem(): Unit =
    Option(
      inventoryTable.selectionModel().selectedItem.value
    ) match
      case Some(foodItem) =>
        if confirmDelete(foodItem) then
          repository.delete(foodItem.id) match
            case Success(true) =>
              loadItems()
              statusLabel.text =
                s"${foodItem.name} was deleted successfully."

            case Success(false) =>
              statusLabel.text =
                "The selected food item was not found."

            case Failure(exception) =>
              statusLabel.text =
                s"Unable to delete item: ${exception.getMessage}"

      case None =>
        statusLabel.text =
          "Select a food item before deleting."

  // ai-assisted: #45
  // why: AI helped protect users from accidental inventory deletion.
  private def confirmDelete(
      foodItem: FoodItem
  ): Boolean =
    val confirmation =
      new Alert(Alert.AlertType.Confirmation):
        title = "Confirm deletion"
        headerText =
          s"Delete ${foodItem.name}?"
        contentText =
          "This removes the selected inventory record. " +
            "This action cannot be undone."

    confirmation
      .showAndWait()
      .contains(ButtonType.OK)

  private def updateSelectedQuantity(): Unit =
    val selectedItem =
      Option(
        inventoryTable.selectionModel().selectedItem.value
      )

    val parsedQuantity =
      Try(newQuantityField.text.value.trim.toInt).toOption

    (selectedItem, parsedQuantity) match
      case (None, _) =>
        statusLabel.text =
          "Select a food item before updating."

      case (_, None) =>
        statusLabel.text =
          "Enter a valid whole-number quantity."

      case (Some(_), Some(quantity)) if quantity <= 0 =>
        statusLabel.text =
          "Quantity must be greater than zero."

      case (Some(foodItem), Some(quantity)) =>
        val updatedItem =
          withQuantity(foodItem, quantity)

        repository.update(updatedItem) match
          case Success(savedItem) =>
            loadItems()
            newQuantityField.clear()
            statusLabel.text =
              s"${savedItem.name} quantity was updated to $quantity."

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to update item: ${exception.getMessage}"

  private def withQuantity(
      foodItem: FoodItem,
      quantity: Int
  ): FoodItem =
    foodItem match
      case perishableFood: PerishableFood =>
        perishableFood.copy(quantity = quantity)

      case shelfStableFood: ShelfStableFood =>
        shelfStableFood.copy(quantity = quantity)
