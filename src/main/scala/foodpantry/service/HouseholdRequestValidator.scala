package foodpantry.service

import foodpantry.model.HouseholdRequest

// ai-assisted: #24
// why: AI helped create immutable validation that returns all household-request errors.
object HouseholdRequestValidator:

  def validate(
      householdRequest: HouseholdRequest
  ): Either[List[String], HouseholdRequest] =
    val errors =
      List(
        Option.when(householdRequest.id.trim.isEmpty)(
          "Request ID is required."
        ),
        Option.when(
          householdRequest.householdName.trim.isEmpty
        )(
          "Household name is required."
        ),
        Option.when(householdRequest.householdSize <= 0)(
          "Household size must be greater than zero."
        ),
        Option.when(
          householdRequest.requestedCategory.trim.isEmpty
        )(
          "Requested category is required."
        ),
        Option.when(
          householdRequest.requestedQuantity <= 0
        )(
          "Requested quantity must be greater than zero."
        )
      ).flatten

    Either.cond(
      errors.isEmpty,
      householdRequest,
      errors
    )