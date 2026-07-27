package foodpantry.ui

import foodpantry.model.{
  FoodItem,
  HouseholdRequest,
  RequestStatus
}
import foodpantry.repository.Repository

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{
  Label,
  TableColumn,
  TableView
}
import scalafx.scene.layout.VBox
import scalafx.scene.layout.{FlowPane, Priority}

import scala.util.{Failure, Success}

// ai-assisted: #26
// why: AI helped build a ScalaFX household-request table using the generic repository.
class HouseholdRequestView(
    private val repository: Repository[HouseholdRequest],
    private val foodItemRepository: Repository[FoodItem]
) extends VBox:

  private val householdRequests =
    ObservableBuffer.empty[HouseholdRequest]

  private val statusLabel =
    UiComponents.statusLabel(
      "Household requests have not been loaded."
    )

  private val requestTable =
    new TableView[HouseholdRequest](householdRequests):
      prefHeight = 390
      placeholder =
        new Label(
          "No household requests yet. Use the form above to add one."
        )
      columns ++= List(
        textColumn(
          "Household",
          householdRequest =>
            householdRequest.householdName
        ),
        textColumn(
          "Household Size",
          householdRequest =>
            householdRequest.householdSize.toString
        ),
        textColumn(
          "Category",
          householdRequest =>
            householdRequest.requestedCategory
        ),
        textColumn(
          "Quantity",
          householdRequest =>
            householdRequest.requestedQuantity.toString
        ),
        textColumn(
          "Request Date",
          householdRequest =>
            householdRequest.requestDate.toString
        ),
        textColumn(
          "Status",
          householdRequest =>
            householdRequest.status.toString
        )
      )

  private val householdRequestForm =
    new HouseholdRequestForm(
      repository,
      foodItemRepository,
      () => loadRequests()
    )

  private val refreshButton =
    UiComponents.secondaryButton("Refresh")

  refreshButton.onAction = handle {
    loadRequests()
  }

  // ai-assisted: #46
  // why: AI helped replace arbitrary statuses with one clear valid request action.
  private val approveRequestButton =
    UiComponents.primaryButton(
      "Approve Selected Request"
    )

  approveRequestButton.onAction = handle {
    approveSelectedRequest()
  }

  private val requestActions =
    new FlowPane:
      hgap = 10
      vgap = 10
      styleClass += "action-bar"
      children = Seq(
        refreshButton,
        approveRequestButton
      )

  // ai-assisted: #45
  // why: AI helped group request actions and explain the workflow more clearly.
  spacing = 14
  padding = Insets(24)
  styleClass += "page"

  VBox.setVgrow(requestTable, Priority.Always)

  children = Seq(
    UiComponents.pageTitle("Household Requests"),
    UiComponents.pageDescription(
      "Register household needs and approve pending requests. " +
        "Fulfilled status is assigned automatically after distribution."
    ),
    UiComponents.formSection(
      "Register a household request",
      householdRequestForm
    ),
    UiComponents.sectionTitle("Request records"),
    requestActions,
    requestTable,
    statusLabel
  )

  def refreshView(): Unit =
    loadRequests()
    householdRequestForm.refreshCategories()

  private def textColumn(
      heading: String,
      extractValue: HouseholdRequest => String
  ): TableColumn[HouseholdRequest, String] =
    new TableColumn[HouseholdRequest, String]:
      text = heading
      cellValueFactory = cellData =>
        StringProperty(
          extractValue(cellData.value)
        )

  private def loadRequests(): Unit =
    repository.findAll() match
      case Success(requests) =>
        householdRequests.clear()
        householdRequests ++= requests

        statusLabel.text =
          s"${requests.size} household request(s) loaded."

      case Failure(exception) =>
        statusLabel.text =
          s"Unable to load requests: ${exception.getMessage}"

  private def approveSelectedRequest(): Unit =
    val selectedRequest =
      Option(
        requestTable.selectionModel().selectedItem.value
      )

    selectedRequest match
      case None =>
        statusLabel.text =
          "Select a pending household request before approving."

      case Some(householdRequest)
          if householdRequest.status ==
            RequestStatus.Approved =>
        statusLabel.text =
          s"${householdRequest.householdName} is already approved."

      case Some(householdRequest)
          if householdRequest.status ==
            RequestStatus.Fulfilled =>
        statusLabel.text =
          s"${householdRequest.householdName} is already fulfilled."

      case Some(householdRequest) =>
        val updatedRequest =
          householdRequest.copy(
            status = RequestStatus.Approved
          )

        repository.update(updatedRequest) match
          case Success(savedRequest) =>
            loadRequests()

            statusLabel.text =
              s"${savedRequest.householdName} was approved " +
                "and is ready for distribution planning."

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to approve request: ${exception.getMessage}"
