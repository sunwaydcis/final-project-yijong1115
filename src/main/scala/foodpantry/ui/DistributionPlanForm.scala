package foodpantry.ui

import foodpantry.model.{
  DistributionPlan,
  DistributionStatus,
  FoodItem,
  HouseholdRequest,
  RequestStatus
}
import foodpantry.repository.Repository
import foodpantry.service.DistributionAllocationService

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
    private val allocationService:
      DistributionAllocationService,
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
      maxWidth = Double.MaxValue

  private val foodItemComboBox =
    new ComboBox[String](foodItemLabels):
      promptText = "Select available food"
      maxWidth = Double.MaxValue

  private val allocatedQuantityField =
    new TextField:
      promptText = "Whole number"

  private val distributionDatePicker =
    new DatePicker:
      value = LocalDate.now.plusDays(1)
      maxWidth = Double.MaxValue

  private val statusLabel =
    UiComponents.statusLabel(
      "Only approved requests and unallocated food are shown."
    )

  private val refreshChoicesButton =
    UiComponents.secondaryButton("Refresh Choices")

  private val createPlanButton =
    UiComponents.primaryButton(
      "Create Distribution Plan"
    )

  refreshChoicesButton.onAction = handle {
    loadChoices()
  }

  createPlanButton.defaultButton = true
  createPlanButton.onAction = handle {
    savePlan()
  }

  // ai-assisted: #45
  // why: AI helped clarify allocation choices and improve form responsiveness.
  hgap = 12
  vgap = 11
  padding = Insets(14)
  styleClass += "form-grid"

  val labelColumn =
    new ColumnConstraints:
      minWidth = 160

  val inputColumn =
    new ColumnConstraints:
      minWidth = 340
      hgrow = Priority.Always
      fillWidth = true

  columnConstraints ++=
    Seq(labelColumn, inputColumn)

  add(
    UiComponents.fieldLabel("Household request"),
    0,
    0
  )
  add(requestComboBox, 1, 0)

  add(UiComponents.fieldLabel("Food item"), 0, 1)
  add(foodItemComboBox, 1, 1)

  add(
    UiComponents.fieldLabel("Allocated quantity"),
    0,
    2
  )
  add(allocatedQuantityField, 1, 2)

  add(
    UiComponents.fieldLabel("Distribution date"),
    0,
    3
  )
  add(distributionDatePicker, 1, 3)

  add(refreshChoicesButton, 0, 4)
  add(createPlanButton, 1, 4)

  add(statusLabel, 0, 5, 2, 1)

  loadChoices()

  private def loadChoices(): Unit =
    (
      requestRepository.findAll(),
      foodItemRepository.findAll(),
      distributionRepository.findAll()
    ) match
      case (
            Success(householdRequests),
            Success(foodItems),
            Success(distributionPlans)
          ) =>
        // ai-assisted: #44
        // why: AI helped show remaining demand and stock after active allocations.
        val activePlans =
          distributionPlans.filter(
            distributionPlan =>
              distributionPlan.status !=
                DistributionStatus.Cancelled
          )

        val allocatedByRequest =
          activePlans.groupMapReduce(
            distributionPlan =>
              distributionPlan.householdRequestId
          )(
            distributionPlan =>
              distributionPlan.allocatedQuantity
          )(
            (firstQuantity, secondQuantity) =>
              firstQuantity + secondQuantity
          )

        val allocatedByFoodItem =
          activePlans.groupMapReduce(
            distributionPlan =>
              distributionPlan.foodItemId
          )(
            distributionPlan =>
              distributionPlan.allocatedQuantity
          )(
            (firstQuantity, secondQuantity) =>
              firstQuantity + secondQuantity
          )

        val availableRequests =
          householdRequests.filter(
            householdRequest =>
              householdRequest.status ==
                RequestStatus.Approved
          ).map: householdRequest =>
            val remainingQuantity =
              householdRequest.requestedQuantity -
                allocatedByRequest.getOrElse(
                  householdRequest.id,
                  0
                )

            householdRequest -> remainingQuantity
          .filter(
            requestWithRemainingQuantity =>
              requestWithRemainingQuantity._2 > 0
          )

        val availableFoodItems =
          foodItems.map: foodItem =>
            val availableQuantity =
              foodItem.quantity -
                allocatedByFoodItem.getOrElse(
                  foodItem.id,
                  0
                )

            foodItem -> availableQuantity
          .filter(
            foodItemWithAvailableQuantity =>
              foodItemWithAvailableQuantity._2 > 0
          )

        requestOptions.clear()
        requestOptions ++=
          availableRequests.map(
            requestWithRemainingQuantity =>
              requestWithRemainingQuantity._1
          )

        requestLabels.clear()
        requestLabels ++=
          availableRequests.map:
            case (
                  householdRequest,
                  remainingQuantity
                ) =>
            s"${householdRequest.householdName} - " +
              s"${householdRequest.requestedCategory} " +
              s"($remainingQuantity remaining)"

        foodItemOptions.clear()
        foodItemOptions ++=
          availableFoodItems.map(
            foodItemWithAvailableQuantity =>
              foodItemWithAvailableQuantity._1
          )

        foodItemLabels.clear()
        foodItemLabels ++=
          availableFoodItems.map:
            case (foodItem, availableQuantity) =>
            s"${foodItem.name} - ${foodItem.category} " +
              s"($availableQuantity ${foodItem.unit} available)"

        requestComboBox
          .selectionModel()
          .clearSelection()

        foodItemComboBox
          .selectionModel()
          .clearSelection()

        statusLabel.text =
          s"${availableRequests.size} approved request(s) and " +
            s"${availableFoodItems.size} food item(s) available."

      case (Failure(exception), _, _) =>
        statusLabel.text =
          s"Unable to load requests: ${exception.getMessage}"

      case (_, Failure(exception), _) =>
        statusLabel.text =
          s"Unable to load food items: ${exception.getMessage}"

      case (_, _, Failure(exception)) =>
        statusLabel.text =
          s"Unable to load distribution plans: ${exception.getMessage}"

  private def savePlan(): Unit =
    createAllocation() match
      case Left(message) =>
        statusLabel.text = message

      case Right(
            (
              householdRequest,
              foodItem,
              allocatedQuantity,
              distributionDate
            )
          ) =>
        // ai-assisted: #44
        // why: AI helped replace direct saving with one atomic allocation operation.
        allocationService
          .allocate(
            householdRequest.id,
            foodItem.id,
            allocatedQuantity,
            distributionDate
          ) match
          case Success(savedPlan) =>
            statusLabel.text =
              s"Plan for ${savedPlan.householdName} " +
                "was created successfully."

            clearForm()
            loadChoices()
            onSaved()

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to create plan: ${exception.getMessage}"

  private def createAllocation(): Either[
    String,
    (HouseholdRequest, FoodItem, Int, LocalDate)
  ] =
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

    yield
      (
        householdRequest,
        foodItem,
        allocatedQuantity,
        distributionDate
      )

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
