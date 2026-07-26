package foodpantry.ui

import foodpantry.model.{
  HouseholdRequest,
  RequestStatus
}
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.{
  DatePicker,
  TextField
}
import scalafx.scene.layout.{
  ColumnConstraints,
  GridPane,
  Priority
}

import java.time.LocalDate
import java.util.UUID
import scala.util.{Failure, Success, Try}

// ai-assisted: #27
// why: AI helped create a safe ScalaFX form for persistent household requests.
class HouseholdRequestForm(
    private val repository: Repository[HouseholdRequest],
    private val onSaved: () => Unit
) extends GridPane:

  private val householdNameField =
    new TextField:
      promptText = "Example: Ahmad family"

  private val householdSizeField =
    new TextField:
      promptText = "Whole number of people"

  private val requestedCategoryField =
    new TextField:
      promptText = "Example: Grains"

  private val requestedQuantityField =
    new TextField:
      promptText = "Whole number"

  private val requestDatePicker =
    new DatePicker:
      value = LocalDate.now
      maxWidth = Double.MaxValue

  private val statusLabel =
    UiComponents.statusLabel(
      "New requests begin with Pending status."
    )

  private val addButton =
    UiComponents.primaryButton("Add Household Request")

  addButton.defaultButton = true
  addButton.onAction = handle {
    saveRequest()
  }

  // ai-assisted: #45
  // why: AI helped improve request-form guidance and responsive layout.
  hgap = 12
  vgap = 11
  padding = Insets(14)
  styleClass += "form-grid"

  val labelColumn =
    new ColumnConstraints:
      minWidth = 150

  val inputColumn =
    new ColumnConstraints:
      minWidth = 260
      hgrow = Priority.Always
      fillWidth = true

  columnConstraints ++=
    Seq(labelColumn, inputColumn)

  add(UiComponents.fieldLabel("Household name"), 0, 0)
  add(householdNameField, 1, 0)

  add(UiComponents.fieldLabel("Household size"), 0, 1)
  add(householdSizeField, 1, 1)

  add(UiComponents.fieldLabel("Food category"), 0, 2)
  add(requestedCategoryField, 1, 2)

  add(UiComponents.fieldLabel("Requested quantity"), 0, 3)
  add(requestedQuantityField, 1, 3)

  add(UiComponents.fieldLabel("Request date"), 0, 4)
  add(requestDatePicker, 1, 4)

  add(addButton, 1, 5)
  add(statusLabel, 0, 6, 2, 1)

  private def saveRequest(): Unit =
    createRequest() match
      case Left(message) =>
        statusLabel.text = message

      case Right(householdRequest) =>
        repository.add(householdRequest) match
          case Success(savedRequest) =>
            statusLabel.text =
              s"Request for ${savedRequest.householdName} " +
                "was added successfully."

            clearForm()
            onSaved()

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to add request: ${exception.getMessage}"

  private def createRequest()
      : Either[String, HouseholdRequest] =
    for
      householdSize <-
        parsePositiveInteger(
          householdSizeField.text.value,
          "Household size"
        )

      requestedQuantity <-
        parsePositiveInteger(
          requestedQuantityField.text.value,
          "Requested quantity"
        )

      requestDate <-
        Option(requestDatePicker.value.value)
          .toRight("Request date is required.")
    yield
      HouseholdRequest(
        UUID.randomUUID().toString,
        householdNameField.text.value.trim,
        householdSize,
        requestedCategoryField.text.value.trim,
        requestedQuantity,
        requestDate,
        RequestStatus.Pending
      )

  private def parsePositiveInteger(
      input: String,
      fieldName: String
  ): Either[String, Int] =
    Try(input.trim.toInt)
      .toEither
      .left
      .map(_ => s"$fieldName must be a whole number.")
      .flatMap: parsedValue =>
        Either.cond(
          parsedValue > 0,
          parsedValue,
          s"$fieldName must be greater than zero."
        )

  private def clearForm(): Unit =
    householdNameField.clear()
    householdSizeField.clear()
    requestedCategoryField.clear()
    requestedQuantityField.clear()
    requestDatePicker.value = LocalDate.now
