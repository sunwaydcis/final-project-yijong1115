package foodpantry.model

import java.time.LocalDate

// ai-assisted: #30
// why: AI helped define an immutable distribution-plan model linked to requests and inventory.
enum DistributionStatus:
  case Planned
  case Completed
  case Cancelled

case class DistributionPlan(
    id: String,
    householdRequestId: String,
    householdName: String,
    foodItemId: String,
    foodItemName: String,
    allocatedQuantity: Int,
    allocatedUnit: String,
    distributionDate: LocalDate,
    status: DistributionStatus
) extends Entity:

  def summary: String =
    s"$householdName will receive $allocatedQuantity " +
      s"$allocatedUnit of $foodItemName on $distributionDate."