package foodpantry.ui

import foodpantry.model.{
  HouseholdRequest,
  RequestStatus
}
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.{
  Button,
  DatePicker,
  Label,
  TextField
}
import scalafx.scene.layout.GridPane

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
      promptText = "Household name"

  private val householdSizeField =
    new TextField:
      promptText = "Number of household members"

  private val requestedCategoryField =
    new TextField:
      promptText = "Requested food category"

  private val requestedQuantityField =
    new TextField:
      promptText = "Requested quantity"

  private val requestDatePicker =
    new DatePicker:
      value = LocalDate.now

  private val statusLabel =
    new Label("Enter the household request details.")

  private val addButton =
    new Button("Add Household Request"):
      defaultButton = true
      onAction = handle {
        saveRequest()
      }

  hgap = 10
  vgap = 10
  padding = Insets(10)

  add(new Label("Household Name"), 0, 0)
  add(householdNameField, 1, 0)

  add(new Label("Household Size"), 0, 1)
  add(householdSizeField, 1, 1)

  add(new Label("Requested Category"), 0, 2)
  add(requestedCategoryField, 1, 2)

  add(new Label("Requested Quantity"), 0, 3)
  add(requestedQuantityField, 1, 3)

  add(new Label("Request Date"), 0, 4)
  add(requestDatePicker, 1, 4)

  add(addButton, 1, 5)
  add(statusLabel, 1, 6)

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