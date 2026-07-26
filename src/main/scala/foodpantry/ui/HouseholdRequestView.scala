package foodpantry.ui

import foodpantry.model.{
  HouseholdRequest,
  RequestStatus
}
import foodpantry.repository.Repository

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
import scalafx.scene.layout.VBox
import scalafx.scene.layout.{FlowPane, Priority}

import scala.util.{Failure, Success}

// ai-assisted: #26
// why: AI helped build a ScalaFX household-request table using the generic repository.
class HouseholdRequestView(
    private val repository: Repository[HouseholdRequest]
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
      () => loadRequests()
    )

  private val refreshButton =
    UiComponents.secondaryButton("Refresh")

  refreshButton.onAction = handle {
    loadRequests()
  }

  private val statusComboBox =
    new ComboBox[String](
      ObservableBuffer(
        "Pending",
        "Approved",
        "Fulfilled"
      )
    ):
      promptText = "Select new status"

  // ai-assisted: #29
  // why: AI helped add safe status updates for selected immutable requests.
  private val updateStatusButton =
    UiComponents.primaryButton("Update Status")

  updateStatusButton.onAction = handle {
    updateSelectedStatus()
  }

  private val requestActions =
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
  // why: AI helped group request actions and explain the workflow more clearly.
  spacing = 14
  padding = Insets(24)
  styleClass += "page"

  VBox.setVgrow(requestTable, Priority.Always)

  children = Seq(
    UiComponents.pageTitle("Household Requests"),
    UiComponents.pageDescription(
      "Register household needs, then approve requests before planning distributions."
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

  loadRequests()

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

  private def updateSelectedStatus(): Unit =
    val selectedRequest =
      Option(
        requestTable.selectionModel().selectedItem.value
      )

    val selectedStatus =
      Option(statusComboBox.value.value)
        .flatMap(parseStatus)

    (selectedRequest, selectedStatus) match
      case (None, _) =>
        statusLabel.text =
          "Select a household request before updating."

      case (_, None) =>
        statusLabel.text =
          "Select a valid request status."

      case (
            Some(householdRequest),
            Some(requestStatus)
          ) =>
        val updatedRequest =
          householdRequest.copy(
            status = requestStatus
          )

        repository.update(updatedRequest) match
          case Success(savedRequest) =>
            loadRequests()
            statusComboBox.selectionModel().clearSelection()

            statusLabel.text =
              s"${savedRequest.householdName} status was " +
                s"updated to ${savedRequest.status}."

          case Failure(exception) =>
            statusLabel.text =
              s"Unable to update request: ${exception.getMessage}"

  private def parseStatus(
      statusValue: String
  ): Option[RequestStatus] =
    statusValue match
      case "Pending" =>
        Some(RequestStatus.Pending)

      case "Approved" =>
        Some(RequestStatus.Approved)

      case "Fulfilled" =>
        Some(RequestStatus.Fulfilled)

      case _ =>
        None
