package foodpantry.service

import foodpantry.model.{
  DistributionPlan,
  DistributionStatus,
  FoodItem,
  HouseholdRequest,
  RequestStatus
}

import java.time.LocalDate
import java.util.UUID

// ai-assisted: #35
// why: AI helped separate distribution allocation rules from the ScalaFX interface.
class DistributionPlanningService:

  def createPlan(
      householdRequest: HouseholdRequest,
      foodItem: FoodItem,
      allocatedQuantity: Int,
      distributionDate: LocalDate,
      today: LocalDate = LocalDate.now
  ): Either[List[String], DistributionPlan] =
    createPlanWithAvailability(
      householdRequest,
      foodItem,
      allocatedQuantity,
      distributionDate,
      foodItem.quantity,
      householdRequest.requestedQuantity,
      today
    )

  // ai-assisted: #44
  // why: AI helped validate against live unallocated stock and remaining demand.
  def createPlanWithAvailability(
      householdRequest: HouseholdRequest,
      foodItem: FoodItem,
      allocatedQuantity: Int,
      distributionDate: LocalDate,
      availableFoodQuantity: Int,
      remainingRequestedQuantity: Int,
      today: LocalDate = LocalDate.now
  ): Either[List[String], DistributionPlan] =
    val errors =
      validationErrors(
        householdRequest,
        foodItem,
        allocatedQuantity,
        distributionDate,
        availableFoodQuantity,
        remainingRequestedQuantity,
        today
      )

    Either.cond(
      errors.isEmpty,
      DistributionPlan(
        UUID.randomUUID().toString,
        householdRequest.id,
        householdRequest.householdName,
        foodItem.id,
        foodItem.name,
        allocatedQuantity,
        foodItem.unit,
        distributionDate,
        DistributionStatus.Planned
      ),
      errors
    )

  private def validationErrors(
      householdRequest: HouseholdRequest,
      foodItem: FoodItem,
      allocatedQuantity: Int,
      distributionDate: LocalDate,
      availableFoodQuantity: Int,
      remainingRequestedQuantity: Int,
      today: LocalDate
  ): List[String] =
    List(
      Option.when(allocatedQuantity <= 0)(
        "Allocated quantity must be greater than zero."
      ),
      Option.when(
        allocatedQuantity > availableFoodQuantity
      )(
        s"Only $availableFoodQuantity ${foodItem.unit} " +
          s"of ${foodItem.name} are available."
      ),
      Option.when(
        allocatedQuantity >
          remainingRequestedQuantity
      )(
        "Allocated quantity cannot exceed the " +
          s"remaining requested quantity of $remainingRequestedQuantity."
      ),
      Option.when(
        !foodItem.category.equalsIgnoreCase(
          householdRequest.requestedCategory
        )
      )(
        s"${foodItem.name} does not match the requested " +
          s"category ${householdRequest.requestedCategory}."
      ),
      Option.when(
        householdRequest.status != RequestStatus.Approved
      )(
        "Only an approved household request can receive " +
          "a distribution plan."
      ),
      Option.when(distributionDate.isBefore(today))(
        "Distribution date cannot be in the past."
      )
    ).flatten
