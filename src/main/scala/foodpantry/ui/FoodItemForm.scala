package foodpantry.ui

import foodpantry.model.{FoodItem, PerishableFood, ShelfStableFood}
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  Button,
  ComboBox,
  DatePicker,
  Label,
  TextField
}
import scalafx.scene.layout.GridPane

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
    )

  private val nameField =
    new TextField:
      promptText = "Food name"

  private val categoryField =
    new TextField:
      promptText = "Category"

  private val quantityField =
    new TextField:
      promptText = "Quantity"

  private val unitField =
    new TextField:
      promptText = "Unit, for example cartons"

  private val expiryDatePicker =
    new DatePicker:
      promptText = "Expiry or best-before date"

  private val statusLabel =
    new Label("Enter the food item details.")

  private val addButton =
    new Button("Add Food Item"):
      defaultButton = true
      onAction = handle {
        saveFoodItem()
      }

  itemTypeComboBox.selectionModel().selectFirst()

  hgap = 10
  vgap = 10
  padding = Insets(10)

  add(new Label("Type"), 0, 0)
  add(itemTypeComboBox, 1, 0)

  add(new Label("Name"), 0, 1)
  add(nameField, 1, 1)

  add(new Label("Category"), 0, 2)
  add(categoryField, 1, 2)

  add(new Label("Quantity"), 0, 3)
  add(quantityField, 1, 3)

  add(new Label("Unit"), 0, 4)
  add(unitField, 1, 4)

  add(new Label("Expiry Date"), 0, 5)
  add(expiryDatePicker, 1, 5)

  add(addButton, 1, 6)
  add(statusLabel, 1, 7)

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