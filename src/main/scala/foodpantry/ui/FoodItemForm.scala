package foodpantry.ui

import foodpantry.model.{FoodItem, PerishableFood, ShelfStableFood}
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  ComboBox,
  DatePicker,
  TextField
}
import scalafx.scene.layout.{
  ColumnConstraints,
  GridPane,
  Priority
}

import java.util.UUID
import scala.util.{Failure, Success, Try}

// ai-assisted: #15
// why: AI helped create a safe ScalaFX form for adding immutable food items.
class FoodItemForm(
    private val repository: Repository[FoodItem],
    private val onSaved: () => Unit
) extends GridPane:

  private val itemTypeComboBox =
    new ComboBox[String](
      ObservableBuffer("Perishable", "Shelf Stable")
    ):
      maxWidth = Double.MaxValue
      promptText = "Choose food type"

  private val nameField =
    new TextField:
      promptText = "Example: Rice pack"

  private val categoryField =
    new TextField:
      promptText = "Example: Grains"

  private val quantityField =
    new TextField:
      promptText = "Whole number"

  private val unitField =
    new TextField:
      promptText = "Example: packs or cartons"

  private val expiryDatePicker =
    new DatePicker:
      promptText = "Expiry or best-before date"
      maxWidth = Double.MaxValue

  private val statusLabel =
    UiComponents.statusLabel(
      "Complete the fields below, then select Add Food Item."
    )

  private val addButton =
    UiComponents.primaryButton("Add Food Item")

  addButton.defaultButton = true
  addButton.onAction = handle {
    saveFoodItem()
  }

  itemTypeComboBox.selectionModel().selectFirst()

  // ai-assisted: #45
  // why: AI helped improve form guidance, spacing and responsive field sizing.
  hgap = 12
  vgap = 11
  padding = Insets(14)
  styleClass += "form-grid"

  val labelColumn =
    new ColumnConstraints:
      minWidth = 125

  val inputColumn =
    new ColumnConstraints:
      minWidth = 260
      hgrow = Priority.Always
      fillWidth = true

  columnConstraints ++=
    Seq(labelColumn, inputColumn)

  add(UiComponents.fieldLabel("Type"), 0, 0)
  add(itemTypeComboBox, 1, 0)

  add(UiComponents.fieldLabel("Food name"), 0, 1)
  add(nameField, 1, 1)

  add(UiComponents.fieldLabel("Category"), 0, 2)
  add(categoryField, 1, 2)

  add(UiComponents.fieldLabel("Quantity"), 0, 3)
  add(quantityField, 1, 3)

  add(UiComponents.fieldLabel("Unit"), 0, 4)
  add(unitField, 1, 4)

  add(UiComponents.fieldLabel("Expiry date"), 0, 5)
  add(expiryDatePicker, 1, 5)

  add(addButton, 1, 6)
  add(statusLabel, 0, 7, 2, 1)

  private def saveFoodItem(): Unit =
    createFoodItem() match
      case Left(message) =>
        statusLabel.text = message

      case Right(foodItem) =>
        repository.add(foodItem) match
          case Success(savedItem) =>
            statusLabel.text =
              s"${savedItem.name} was added successfully."
            clearForm()
            onSaved()

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to add item: ${exception.getMessage}"

  private def createFoodItem(): Either[String, FoodItem] =
    Try(quantityField.text.value.trim.toInt)
      .toEither
      .left
      .map(_ => "Quantity must be a whole number.")
      .flatMap: quantity =>
        val id = UUID.randomUUID().toString
        val name = nameField.text.value.trim
        val category = categoryField.text.value.trim
        val unit = unitField.text.value.trim

        itemTypeComboBox.value.value match
          case "Perishable" =>
            Option(expiryDatePicker.value.value)
              .toRight(
                "Perishable food requires an expiry date."
              )
              .map: expiryDate =>
                PerishableFood(
                  id,
                  name,
                  category,
                  quantity,
                  unit,
                  expiryDate
                )

          case "Shelf Stable" =>
            Right(
              ShelfStableFood(
                id,
                name,
                category,
                quantity,
                unit,
                Option(expiryDatePicker.value.value)
              )
            )

          case _ =>
            Left("Please select a food item type.")

  private def clearForm(): Unit =
    nameField.clear()
    categoryField.clear()
    quantityField.clear()
    unitField.clear()
    expiryDatePicker.value = null
    itemTypeComboBox.selectionModel().selectFirst()
