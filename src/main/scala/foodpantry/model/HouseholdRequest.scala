package foodpantry.model

import java.time.LocalDate

// ai-assisted: #21
// why: AI helped define an immutable household food-request model with clear statuses.
enum RequestStatus:
  case Pending
  case Approved
  case Rejected
  case Fulfilled

case class HouseholdRequest(
    id: String,
    householdName: String,
    householdSize: Int,
    requestedCategory: String,
    requestedQuantity: Int,
    requestDate: LocalDate,
    status: RequestStatus
) extends Entity:

  def summary: String =
    s"$householdName requests $requestedQuantity " +
      s"$requestedCategory item(s) for $householdSize people."
