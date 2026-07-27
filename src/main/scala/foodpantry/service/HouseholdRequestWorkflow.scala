package foodpantry.service

import foodpantry.model.{
  HouseholdRequest,
  RequestStatus
}

// ai-assisted: #52
// why: AI helped keep valid approval and rejection transitions outside the UI.
object HouseholdRequestWorkflow:

  def approve(
      householdRequest: HouseholdRequest
  ): Either[String, HouseholdRequest] =
    householdRequest.status match
      case RequestStatus.Pending =>
        Right(
          householdRequest.copy(
            status = RequestStatus.Approved
          )
        )

      case RequestStatus.Approved =>
        Left("This household request is already approved.")

      case RequestStatus.Rejected =>
        Left(
          "A rejected household request cannot be approved."
        )

      case RequestStatus.Fulfilled =>
        Left("This household request is already fulfilled.")

  def reject(
      householdRequest: HouseholdRequest
  ): Either[String, HouseholdRequest] =
    householdRequest.status match
      case RequestStatus.Pending =>
        Right(
          householdRequest.copy(
            status = RequestStatus.Rejected
          )
        )

      case RequestStatus.Approved =>
        Left(
          "An approved request cannot be rejected. " +
            "Only pending requests can be rejected."
        )

      case RequestStatus.Rejected =>
        Left("This household request is already rejected.")

      case RequestStatus.Fulfilled =>
        Left("A fulfilled request cannot be rejected.")
