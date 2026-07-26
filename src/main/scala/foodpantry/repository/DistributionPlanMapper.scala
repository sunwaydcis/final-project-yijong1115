package foodpantry.repository

import foodpantry.model.{
  DistributionPlan,
  DistributionStatus
}

import java.sql.ResultSet

// ai-assisted: #32
// why: AI helped map Derby rows safely into immutable distribution plans.
object DistributionPlanMapper:

  def fromResultSet(
      resultSet: ResultSet
  ): Either[String, DistributionPlan] =
    val id =
      resultSet.getString("ID")

    val householdRequestId =
      resultSet.getString("HOUSEHOLD_REQUEST_ID")

    val householdName =
      resultSet.getString("HOUSEHOLD_NAME")

    val foodItemId =
      resultSet.getString("FOOD_ITEM_ID")

    val foodItemName =
      resultSet.getString("FOOD_ITEM_NAME")

    val allocatedQuantity =
      resultSet.getInt("ALLOCATED_QUANTITY")

    val allocatedUnit =
      resultSet.getString("ALLOCATED_UNIT")

    val distributionDateResult =
      Option(resultSet.getDate("DISTRIBUTION_DATE"))
        .map(databaseDate => databaseDate.toLocalDate)
        .toRight(
          s"Distribution plan $id requires a distribution date."
        )

    val statusResult =
      parseStatus(resultSet.getString("STATUS"))

    for
      distributionDate <- distributionDateResult
      distributionStatus <- statusResult
    yield
      DistributionPlan(
        id,
        householdRequestId,
        householdName,
        foodItemId,
        foodItemName,
        allocatedQuantity,
        allocatedUnit,
        distributionDate,
        distributionStatus
      )

  private def parseStatus(
      statusValue: String
  ): Either[String, DistributionStatus] =
    statusValue match
      case "Planned" =>
        Right(DistributionStatus.Planned)

      case "Completed" =>
        Right(DistributionStatus.Completed)

      case "Cancelled" =>
        Right(DistributionStatus.Cancelled)

      case unsupportedStatus =>
        Left(
          s"Unsupported distribution status: $unsupportedStatus"
        )