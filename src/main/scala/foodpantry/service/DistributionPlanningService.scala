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
    val errors =
      validationErrors(
        householdRequest,
        foodItem,
        allocatedQuantity,
        distributionDate,
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
      today: LocalDate
  ): List[String] =
    List(
      Option.when(allocatedQuantity <= 0)(
        "Allocated quantity must be greater than zero."
      ),
      Option.when(
        allocatedQuantity > foodItem.quantity
      )(
        s"Only ${foodItem.quantity} ${foodItem.unit} " +
          s"of ${foodItem.name} are available."
      ),
      Option.when(
        allocatedQuantity >
          householdRequest.requestedQuantity
      )(
        "Allocated quantity cannot exceed the " +
          "household's requested quantity."
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
        householdRequest.status == RequestStatus.Fulfilled
      )(
        "A fulfilled household request cannot receive " +
          "another distribution plan."
      ),
      Option.when(distributionDate.isBefore(today))(
        "Distribution date cannot be in the past."
      )
    ).flatten