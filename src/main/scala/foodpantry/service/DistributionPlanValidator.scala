package foodpantry.service

import foodpantry.model.DistributionPlan

// ai-assisted: #33
// why: AI helped create immutable validation that returns all distribution-plan errors.
object DistributionPlanValidator:

  def validate(
      distributionPlan: DistributionPlan
  ): Either[List[String], DistributionPlan] =
    val errors =
      List(
        Option.when(distributionPlan.id.trim.isEmpty)(
          "Distribution plan ID is required."
        ),
        Option.when(
          distributionPlan.householdRequestId.trim.isEmpty
        )(
          "Household request ID is required."
        ),
        Option.when(
          distributionPlan.householdName.trim.isEmpty
        )(
          "Household name is required."
        ),
        Option.when(
          distributionPlan.foodItemId.trim.isEmpty
        )(
          "Food item ID is required."
        ),
        Option.when(
          distributionPlan.foodItemName.trim.isEmpty
        )(
          "Food item name is required."
        ),
        Option.when(
          distributionPlan.allocatedQuantity <= 0
        )(
          "Allocated quantity must be greater than zero."
        ),
        Option.when(
          distributionPlan.allocatedUnit.trim.isEmpty
        )(
          "Allocated unit is required."
        )
      ).flatten

    Either.cond(
      errors.isEmpty,
      distributionPlan,
      errors
    )