package foodpantry.ui

import foodpantry.model.{
  DistributionPlan,
  FoodItem,
  HouseholdRequest,
  RequestStatus
}
import foodpantry.repository.Repository
import foodpantry.service.DistributionPlanningService

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

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

// ai-assisted: #37
// why: AI helped connect approved requests and available food to safe plan creation.
class DistributionPlanForm(
    private val requestRepository:
      Repository[HouseholdRequest],
    private val foodItemRepository:
      Repository[FoodItem],
    private val distributionRepository:
      Repository[DistributionPlan],
    private val planningService:
      DistributionPlanningService,
    private val onSaved: () => Unit
) extends GridPane:

  private val requestOptions =
    ObservableBuffer.empty[HouseholdRequest]

  private val requestLabels =
    ObservableBuffer.empty[String]

  private val foodItemOptions =
    ObservableBuffer.empty[FoodItem]

  private val foodItemLabels =
    ObservableBuffer.empty[String]

  private val requestComboBox =
    new ComboBox[String](requestLabels):
      promptText = "Select an approved request"

  private val foodItemComboBox =
    new ComboBox[String](foodItemLabels):
      promptText = "Select available food"

  private val allocatedQuantityField =
    new TextField:
      promptText = "Allocated quantity"

  private val distributionDatePicker =
    new DatePicker:
      value = LocalDate.now.plusDays(1)

  private val statusLabel =
    new Label("Select a request and food item.")

  private val refreshChoicesButton =
    new Button("Refresh Choices"):
      onAction = handle {
        loadChoices()
      }

  private val createPlanButton =
    new Button("Create Distribution Plan"):
      defaultButton = true
      onAction = handle {
        savePlan()
      }

  hgap = 10
  vgap = 10
  padding = Insets(10)

  add(new Label("Household Request"), 0, 0)
  add(requestComboBox, 1, 0)

  add(new Label("Food Item"), 0, 1)
  add(foodItemComboBox, 1, 1)

  add(new Label("Allocated Quantity"), 0, 2)
  add(allocatedQuantityField, 1, 2)

  add(new Label("Distribution Date"), 0, 3)
  add(distributionDatePicker, 1, 3)

  add(refreshChoicesButton, 0, 4)
  add(createPlanButton, 1, 4)

  add(statusLabel, 1, 5)

  loadChoices()

  private def loadChoices(): Unit =
    (
      requestRepository.findAll(),
      foodItemRepository.findAll()
    ) match
      case (
            Success(householdRequests),
            Success(foodItems)
          ) =>
        val approvedRequests =
          householdRequests.filter(
            householdRequest =>
              householdRequest.status ==
                RequestStatus.Approved
          )

        val availableFoodItems =
          foodItems.filter(
            foodItem => foodItem.quantity > 0
          )

        requestOptions.clear()
        requestOptions ++= approvedRequests

        requestLabels.clear()
        requestLabels ++=
          approvedRequests.map: householdRequest =>
            s"${householdRequest.householdName} - " +
              s"${householdRequest.requestedCategory} " +
              s"(${householdRequest.requestedQuantity})"

        foodItemOptions.clear()
        foodItemOptions ++= availableFoodItems

        foodItemLabels.clear()
        foodItemLabels ++=
          availableFoodItems.map: foodItem =>
            s"${foodItem.name} - ${foodItem.category} " +
              s"(${foodItem.quantity} ${foodItem.unit})"

        requestComboBox
          .selectionModel()
          .clearSelection()

        foodItemComboBox
          .selectionModel()
          .clearSelection()

        statusLabel.text =
          s"${approvedRequests.size} approved request(s) and " +
            s"${availableFoodItems.size} food item(s) available."

      case (Failure(exception), _) =>
        statusLabel.text =
          s"Unable to load requests: ${exception.getMessage}"

      case (_, Failure(exception)) =>
        statusLabel.text =
          s"Unable to load food items: ${exception.getMessage}"

  private def savePlan(): Unit =
    createPlan() match
      case Left(message) =>
        statusLabel.text = message

      case Right(distributionPlan) =>
        distributionRepository
          .add(distributionPlan) match
          case Success(savedPlan) =>
            statusLabel.text =
              s"Plan for ${savedPlan.householdName} " +
                "was created successfully."

            clearForm()
            onSaved()

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to create plan: ${exception.getMessage}"

  private def createPlan()
      : Either[String, DistributionPlan] =
    val selectedHouseholdRequest =
      requestOptions
        .toList
        .lift(
          requestComboBox
            .selectionModel()
            .selectedIndex
            .value
        )

    val selectedFoodItem =
      foodItemOptions
        .toList
        .lift(
          foodItemComboBox
            .selectionModel()
            .selectedIndex
            .value
        )

    for
      householdRequest <-
        selectedHouseholdRequest
          .toRight(
            "Select an approved household request."
          )

      foodItem <-
        selectedFoodItem
          .toRight(
            "Select an available food item."
          )

      allocatedQuantity <-
        parsePositiveInteger(
          allocatedQuantityField.text.value
        )

      distributionDate <-
        Option(distributionDatePicker.value.value)
          .toRight(
            "Distribution date is required."
          )

      distributionPlan <-
        planningService
          .createPlan(
            householdRequest,
            foodItem,
            allocatedQuantity,
            distributionDate
          )
          .left
          .map(errors => errors.mkString(" "))
    yield
      distributionPlan

  private def parsePositiveInteger(
      input: String
  ): Either[String, Int] =
    Try(input.trim.toInt)
      .toEither
      .left
      .map(
        _ =>
          "Allocated quantity must be a whole number."
      )
      .flatMap: parsedQuantity =>
        Either.cond(
          parsedQuantity > 0,
          parsedQuantity,
          "Allocated quantity must be greater than zero."
        )

  private def clearForm(): Unit =
    requestComboBox
      .selectionModel()
      .clearSelection()

    foodItemComboBox
      .selectionModel()
      .clearSelection()

    allocatedQuantityField.clear()

    distributionDatePicker.value =
      LocalDate.now.plusDays(1)