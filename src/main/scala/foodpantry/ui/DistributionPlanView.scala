package foodpantry.ui

import foodpantry.model.{
  DistributionPlan,
  DistributionStatus,
  FoodItem,
  HouseholdRequest
}
import foodpantry.repository.Repository
import foodpantry.service.{
  DistributionAllocationService,
  DistributionReportService
}

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  ComboBox,
  Label,
  TableColumn,
  TableView
}
import scalafx.scene.layout.{FlowPane, Priority, VBox}

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
    private val allocationService:
      DistributionAllocationService
) extends VBox:

  private val distributionPlans =
    ObservableBuffer.empty[DistributionPlan]

  private val statusLabel =
    UiComponents.statusLabel(
      "Distribution plans have not been loaded."
    )

  private val totalPlansLabel =
    new Label("Total plans: 0"):
      styleClass += "summary-card"

  private val plannedPlansLabel =
    new Label("Planned: 0"):
      styleClass += "summary-card"

  private val completedPlansLabel =
    new Label("Completed: 0"):
      styleClass += "summary-card"

  private val cancelledPlansLabel =
    new Label("Cancelled: 0"):
      styleClass += "summary-card"

  private val householdsServedLabel =
    new Label("Households served: 0"):
      styleClass += "summary-card"

  // ai-assisted: #43
  // why: AI helped replace a compressed multiline label with a wrapping summary panel.
  private val reportSummaryPane =
    new FlowPane:
      hgap = 18
      vgap = 6
      prefWrapLength = 850
      minHeight = 55

      children = Seq(
        totalPlansLabel,
        plannedPlansLabel,
        completedPlansLabel,
        cancelledPlansLabel,
        householdsServedLabel
      )

  private val distributionTable =
    new TableView[DistributionPlan](distributionPlans):
      prefHeight = 390
      placeholder =
        new Label(
          "No distribution plans yet. Approved requests will appear in the form above."
        )

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
      allocationService,
      () => loadPlans()
    )

  private val refreshButton =
    UiComponents.secondaryButton("Refresh")

  refreshButton.onAction = handle {
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
    UiComponents.primaryButton("Update Status")

  updateStatusButton.onAction = handle {
    updateSelectedStatus()
  }

  private val planActions =
    new FlowPane:
      hgap = 10
      vgap = 10
      styleClass += "action-bar"
      children = Seq(
        refreshButton,
        statusComboBox,
        updateStatusButton
      )

  // ai-assisted: #45
  // why: AI helped reorganise planning, reporting and status tasks into clear sections.
  spacing = 14
  padding = Insets(24)
  styleClass += "page"

  VBox.setVgrow(distributionTable, Priority.Always)

  children = Seq(
    UiComponents.pageTitle("Distribution Planning"),
    UiComponents.pageDescription(
      "Allocate available food to approved requests and track delivery progress."
    ),
    UiComponents.formSection(
      "Create a distribution plan",
      distributionPlanForm
    ),
    UiComponents.sectionTitle("Distribution summary"),
    reportSummaryPane,
    UiComponents.sectionTitle("Plan records"),
    planActions,
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

        updateReport(plans)

        statusLabel.text =
          s"${plans.size} distribution plan(s) loaded."

      case Failure(exception) =>
        showUnavailableReport()

        statusLabel.text =
          s"Unable to load distribution plans: ${exception.getMessage}"

  // ai-assisted: #42
  // why: AI helped connect the immutable report service to the ScalaFX screen.
  private def updateReport(
      plans: List[DistributionPlan]
  ): Unit =
    val report =
      DistributionReportService.generate(plans)

    totalPlansLabel.text =
      s"Total plans: ${report.totalPlans}"

    plannedPlansLabel.text =
      s"Planned: ${report.plannedPlans}"

    completedPlansLabel.text =
      s"Completed: ${report.completedPlans}"

    cancelledPlansLabel.text =
      s"Cancelled: ${report.cancelledPlans}"

    householdsServedLabel.text =
      s"Households served: ${report.householdsServed}"

  private def showUnavailableReport(): Unit =
    totalPlansLabel.text =
      "Total plans: unavailable"

    plannedPlansLabel.text =
      "Planned: -"

    completedPlansLabel.text =
      "Completed: -"

    cancelledPlansLabel.text =
      "Cancelled: -"

    householdsServedLabel.text =
      "Households served: -"

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
