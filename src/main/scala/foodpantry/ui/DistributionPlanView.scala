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
  Alert,
  ButtonType,
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

  // ai-assisted: #46
  // why: AI helped replace arbitrary plan statuses with clear valid workflow actions.
  private val completePlanButton =
    UiComponents.primaryButton(
      "Complete Selected Plan"
    )

  completePlanButton.onAction = handle {
    completeSelectedPlan()
  }

  private val cancelPlanButton =
    UiComponents.dangerButton(
      "Cancel Selected Plan"
    )

  cancelPlanButton.onAction = handle {
    cancelSelectedPlan()
  }

  private val planActions =
    new FlowPane:
      hgap = 10
      vgap = 10
      styleClass += "action-bar"
      children = Seq(
        refreshButton,
        completePlanButton,
        cancelPlanButton
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
      "Allocate available food, then complete or cancel each planned delivery. " +
        "Completing a plan reduces inventory automatically."
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

  def refreshView(): Unit =
    loadPlans()
    distributionPlanForm.refreshChoices()

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

  private def completeSelectedPlan(): Unit =
    selectedPlan() match
      case None =>
        statusLabel.text =
          "Select a planned distribution before completing it."

      case Some(distributionPlan)
          if distributionPlan.status !=
            DistributionStatus.Planned =>
        statusLabel.text =
          s"This distribution is already ${distributionPlan.status}."

      case Some(distributionPlan) =>
        if confirmCompletion(distributionPlan) then
          allocationService.complete(distributionPlan.id) match
            case Success(completedPlan) =>
              refreshPlansAndChoices()
              statusLabel.text =
                s"Distribution for ${completedPlan.householdName} " +
                  "was completed and inventory was reduced."

            case Failure(exception) =>
              statusLabel.text =
                s"Unable to complete distribution: ${exception.getMessage}"

  private def cancelSelectedPlan(): Unit =
    selectedPlan() match
      case None =>
        statusLabel.text =
          "Select a planned distribution before cancelling it."

      case Some(distributionPlan)
          if distributionPlan.status !=
            DistributionStatus.Planned =>
        statusLabel.text =
          s"This distribution is already ${distributionPlan.status}."

      case Some(distributionPlan) =>
        if confirmCancellation(distributionPlan) then
          allocationService.cancel(distributionPlan.id) match
            case Success(cancelledPlan) =>
              refreshPlansAndChoices()
              statusLabel.text =
                s"Distribution for ${cancelledPlan.householdName} " +
                  "was cancelled and its reservation was released."

            case Failure(exception) =>
              statusLabel.text =
                s"Unable to cancel distribution: ${exception.getMessage}"

  private def selectedPlan(): Option[DistributionPlan] =
    Option(
      distributionTable
        .selectionModel()
        .selectedItem
        .value
    )

  private def refreshPlansAndChoices(): Unit =
    loadPlans()
    distributionPlanForm.refreshChoices()

  private def confirmCompletion(
      distributionPlan: DistributionPlan
  ): Boolean =
    val confirmation =
      new Alert(Alert.AlertType.Confirmation):
        title = "Complete distribution"
        headerText =
          s"Complete delivery for ${distributionPlan.householdName}?"
        contentText =
          s"${distributionPlan.allocatedQuantity} " +
            s"${distributionPlan.allocatedUnit} of " +
            s"${distributionPlan.foodItemName} will be removed " +
            "from inventory. This action cannot be undone."

    confirmation
      .showAndWait()
      .contains(ButtonType.OK)

  private def confirmCancellation(
      distributionPlan: DistributionPlan
  ): Boolean =
    val confirmation =
      new Alert(Alert.AlertType.Confirmation):
        title = "Cancel distribution"
        headerText =
          s"Cancel the plan for ${distributionPlan.householdName}?"
        contentText =
          "The reserved food will become available for another plan. " +
            "This action cannot be undone."

    confirmation
      .showAndWait()
      .contains(ButtonType.OK)
