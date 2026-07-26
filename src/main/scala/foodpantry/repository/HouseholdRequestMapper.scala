package foodpantry.repository

import foodpantry.model.{
  HouseholdRequest,
  RequestStatus
}

import java.sql.ResultSet

// ai-assisted: #23
// why: AI helped map Derby rows safely into immutable household requests.
object HouseholdRequestMapper:

  def fromResultSet(
      resultSet: ResultSet
  ): Either[String, HouseholdRequest] =
    val id =
      resultSet.getString("ID")

    val householdName =
      resultSet.getString("HOUSEHOLD_NAME")

    val householdSize =
      resultSet.getInt("HOUSEHOLD_SIZE")

    val requestedCategory =
      resultSet.getString("REQUESTED_CATEGORY")

    val requestedQuantity =
      resultSet.getInt("REQUESTED_QUANTITY")

    val requestDateResult =
      Option(resultSet.getDate("REQUEST_DATE"))
        .map(databaseDate => databaseDate.toLocalDate)
        .toRight(
          s"Household request $id requires a request date."
        )

    val statusResult =
      parseStatus(resultSet.getString("STATUS"))

    for
      requestDate <- requestDateResult
      requestStatus <- statusResult
    yield
      HouseholdRequest(
        id,
        householdName,
        householdSize,
        requestedCategory,
        requestedQuantity,
        requestDate,
        requestStatus
      )

  private def parseStatus(
      statusValue: String
  ): Either[String, RequestStatus] =
    statusValue match
      case "Pending" =>
        Right(RequestStatus.Pending)

      case "Approved" =>
        Right(RequestStatus.Approved)

      case "Fulfilled" =>
        Right(RequestStatus.Fulfilled)

      case unsupportedStatus =>
        Left(
          s"Unsupported household request status: $unsupportedStatus"
        )