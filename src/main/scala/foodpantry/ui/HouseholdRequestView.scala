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
  Button,
  ComboBox,
  Label,
  TableColumn,
  TableView
}
import scalafx.scene.layout.VBox

import scala.util.{Failure, Success}

// ai-assisted: #26
// why: AI helped build a ScalaFX household-request table using the generic repository.
class HouseholdRequestView(
    private val repository: Repository[HouseholdRequest]
) extends VBox:

  private val householdRequests =
    ObservableBuffer.empty[HouseholdRequest]

  private val statusLabel =
    new Label("Household requests have not been loaded.")

  private val requestTable =
    new TableView[HouseholdRequest](householdRequests):
      prefHeight = 420
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
    new Button("Refresh Requests"):
      onAction = handle {
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
    new Button("Update Selected Status"):
      onAction = handle {
        updateSelectedStatus()
      }

  spacing = 10
  padding = Insets(20)

  children = Seq(
    new Label("Household Food Requests"),
    householdRequestForm,
    refreshButton,
    statusComboBox,
    updateStatusButton,
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