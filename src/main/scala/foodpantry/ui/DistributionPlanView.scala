package foodpantry.ui

import foodpantry.model.{
  DistributionPlan,
  DistributionStatus,
  FoodItem,
  HouseholdRequest
}
import foodpantry.repository.Repository
import foodpantry.service.DistributionPlanningService

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  Button,
  ComboBox,
  Label,
  TableColumn,
  TableView
}
import scalafx.scene.layout.VBox

import scala.util.{Failure, Success}

// ai-assisted: #36
// why: AI helped build a ScalaFX distribution-plan table using the generic repository.
class DistributionPlanView(
    private val distributionRepository:
      Repository[DistributionPlan],
    private val requestRepository:
      Repository[HouseholdRequest],
    private val foodItemRepository:
      Repository[FoodItem],
    private val planningService:
      DistributionPlanningService
) extends VBox:

  private val distributionPlans =
    ObservableBuffer.empty[DistributionPlan]

  private val statusLabel =
    new Label("Distribution plans have not been loaded.")

  private val distributionTable =
    new TableView[DistributionPlan](distributionPlans):
      prefHeight = 350
      columns ++= List(
        textColumn(
          "Household",
          distributionPlan =>
            distributionPlan.householdName
        ),
        textColumn(
          "Food Item",
          distributionPlan =>
            distributionPlan.foodItemName
        ),
        textColumn(
          "Quantity",
          distributionPlan =>
            distributionPlan.allocatedQuantity.toString
        ),
        textColumn(
          "Unit",
          distributionPlan =>
            distributionPlan.allocatedUnit
        ),
        textColumn(
          "Distribution Date",
          distributionPlan =>
            distributionPlan.distributionDate.toString
        ),
        textColumn(
          "Status",
          distributionPlan =>
            distributionPlan.status.toString
        )
      )

  // ai-assisted: #38
  // why: AI helped connect plan creation to automatic table refreshing.
  private val distributionPlanForm =
    new DistributionPlanForm(
      requestRepository,
      foodItemRepository,
      distributionRepository,
      planningService,
      () => loadPlans()
    )

  private val refreshButton =
    new Button("Refresh Distribution Plans"):
      onAction = handle {
        loadPlans()
      }

  private val statusComboBox =
    new ComboBox[String](
      ObservableBuffer(
        "Planned",
        "Completed",
        "Cancelled"
      )
    ):
      promptText = "Select new status"

  // ai-assisted: #40
  // why: AI helped add safe status updates for selected immutable plans.
  private val updateStatusButton =
    new Button("Update Selected Status"):
      onAction = handle {
        updateSelectedStatus()
      }

  spacing = 10
  padding = Insets(20)

  children = Seq(
    new Label("Daily Distribution Plans"),
    distributionPlanForm,
    refreshButton,
    statusComboBox,
    updateStatusButton,
    distributionTable,
    statusLabel
  )

  loadPlans()

  private def textColumn(
      heading: String,
      extractValue: DistributionPlan => String
  ): TableColumn[DistributionPlan, String] =
    new TableColumn[DistributionPlan, String]:
      text = heading
      cellValueFactory = cellData =>
        StringProperty(
          extractValue(cellData.value)
        )

  private def loadPlans(): Unit =
    distributionRepository.findAll() match
      case Success(plans) =>
        distributionPlans.clear()
        distributionPlans ++= plans

        statusLabel.text =
          s"${plans.size} distribution plan(s) loaded."

      case Failure(exception) =>
        statusLabel.text =
          s"Unable to load distribution plans: ${exception.getMessage}"

  private def updateSelectedStatus(): Unit =
    val selectedPlan =
      Option(
        distributionTable
          .selectionModel()
          .selectedItem
          .value
      )

    val selectedStatus =
      Option(statusComboBox.value.value)
        .flatMap(parseStatus)

    (selectedPlan, selectedStatus) match
      case (None, _) =>
        statusLabel.text =
          "Select a distribution plan before updating."

      case (_, None) =>
        statusLabel.text =
          "Select a valid distribution status."

      case (
            Some(distributionPlan),
            Some(distributionStatus)
          ) =>
        val updatedPlan =
          distributionPlan.copy(
            status = distributionStatus
          )

        distributionRepository
          .update(updatedPlan) match
          case Success(savedPlan) =>
            loadPlans()

            statusComboBox
              .selectionModel()
              .clearSelection()

            statusLabel.text =
              s"${savedPlan.householdName} distribution " +
                s"status was updated to ${savedPlan.status}."

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to update plan: ${exception.getMessage}"

  private def parseStatus(
      statusValue: String
  ): Option[DistributionStatus] =
    statusValue match
      case "Planned" =>
        Some(DistributionStatus.Planned)

      case "Completed" =>
        Some(DistributionStatus.Completed)

      case "Cancelled" =>
        Some(DistributionStatus.Cancelled)

      case _ =>
        None