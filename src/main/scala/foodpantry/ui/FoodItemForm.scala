package foodpantry.ui

import foodpantry.model.{
  FoodItem,
  FoodCategories,
  FoodUnits,
  PerishableFood,
  ShelfStableFood
}
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  ComboBox,
  DatePicker,
  Label,
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

  // ai-assisted: #48
  // why: AI helped make each food subtype visibly understandable in the form.
  private val typeGuidanceLabel =
    new Label:
      wrapText = true
      styleClass += "field-guidance"

  private val nameField =
    new TextField:
      promptText = "Example: Rice pack"

  private val categoryField =
    new TextField:
      promptText = "Example: Grains"

  private val quantityField =
    new TextField:
      promptText = "Whole number"

  // ai-assisted: #49
  // why: AI helped prevent invalid unit typing with a guided fixed choice.
  private val unitComboBox =
    new ComboBox[String](
      ObservableBuffer(FoodUnits.choices*)
    ):
      promptText = "Choose how this food is counted"
      maxWidth = Double.MaxValue

  private val expiryDatePicker =
    new DatePicker:
      maxWidth = Double.MaxValue

  private val expiryDateLabel =
    UiComponents.fieldLabel("Use-by date (required)")

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

  itemTypeComboBox.onAction = handle {
    updateTypeGuidance()
  }

  itemTypeComboBox.selectionModel().selectFirst()
  updateTypeGuidance()

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
  add(typeGuidanceLabel, 0, 1, 2, 1)

  add(UiComponents.fieldLabel("Food name"), 0, 2)
  add(nameField, 1, 2)

  add(UiComponents.fieldLabel("Category"), 0, 3)
  add(categoryField, 1, 3)

  add(UiComponents.fieldLabel("Quantity"), 0, 4)
  add(quantityField, 1, 4)

  add(UiComponents.fieldLabel("Unit"), 0, 5)
  add(unitComboBox, 1, 5)

  add(expiryDateLabel, 0, 6)
  add(expiryDatePicker, 1, 6)

  add(addButton, 1, 7)
  add(statusLabel, 0, 8, 2, 1)

  private def updateTypeGuidance(): Unit =
    itemTypeComboBox.value.value match
      case "Perishable" =>
        typeGuidanceLabel.text =
          "Examples: milk, meat and vegetables. " +
            "A use-by date is required and the item is kept refrigerated."
        expiryDateLabel.text =
          "Use-by date (required)"
        expiryDatePicker.promptText =
          "Choose the required use-by date"

      case "Shelf Stable" =>
        typeGuidanceLabel.text =
          "Examples: rice, flour and canned food. " +
            "A best-before date is optional and the item is stored in a cool, dry place."
        expiryDateLabel.text =
          "Best-before date (optional)"
        expiryDatePicker.promptText =
          "Optional best-before date"

      case _ =>
        typeGuidanceLabel.text =
          "Choose a food type to see its storage and date requirements."
        expiryDateLabel.text =
          "Expiry information"
        expiryDatePicker.promptText =
          "Choose a food type first"

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
    for
      quantity <-
        Try(quantityField.text.value.trim.toInt)
          .toEither
          .left
          .map(_ => "Quantity must be a whole number.")

      unit <-
        Option(unitComboBox.value.value)
          .toRight("Choose a food unit.")

      foodItem <-
        createFoodItemForType(quantity, unit)
    yield
      foodItem

  private def createFoodItemForType(
      quantity: Int,
      unit: String
  ): Either[String, FoodItem] =
    val id = UUID.randomUUID().toString
    val name = nameField.text.value.trim
    val category =
      FoodCategories.normalize(categoryField.text.value)

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
    unitComboBox
      .selectionModel()
      .clearSelection()
    expiryDatePicker.value = null
    itemTypeComboBox.selectionModel().selectFirst()
