package foodpantry.ui

import foodpantry.model.DistributionPlan
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  Button,
  Label,
  TableColumn,
  TableView
}
import scalafx.scene.layout.VBox

import scala.util.{Failure, Success}

// ai-assisted: #36
// why: AI helped build a ScalaFX distribution-plan table using the generic repository.
class DistributionPlanView(
    private val repository: Repository[DistributionPlan]
) extends VBox:

  private val distributionPlans =
    ObservableBuffer.empty[DistributionPlan]

  private val statusLabel =
    new Label("Distribution plans have not been loaded.")

  private val distributionTable =
    new TableView[DistributionPlan](distributionPlans):
      prefHeight = 470
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

  private val refreshButton =
    new Button("Refresh Distribution Plans"):
      onAction = handle {
        loadPlans()
      }

  spacing = 10
  padding = Insets(20)

  children = Seq(
    new Label("Daily Distribution Plans"),
    refreshButton,
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
    repository.findAll() match
      case Success(plans) =>
        distributionPlans.clear()
        distributionPlans ++= plans

        statusLabel.text =
          s"${plans.size} distribution plan(s) loaded."

      case Failure(exception) =>
        statusLabel.text =
          s"Unable to load distribution plans: ${exception.getMessage}"