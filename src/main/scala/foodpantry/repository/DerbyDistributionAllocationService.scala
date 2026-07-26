package foodpantry.repository

import foodpantry.database.DatabaseManager
import foodpantry.model.{
  DistributionPlan,
  DistributionStatus,
  FoodItem,
  HouseholdRequest
}
import foodpantry.service.{
  DistributionAllocationService,
  DistributionPlanningService
}

import java.sql.{Connection, Date, PreparedStatement}
import java.time.LocalDate
import scala.util.{Failure, Success, Try}

// ai-assisted: #44
// why: AI helped make plan creation atomic and recheck remaining demand and stock.
class DerbyDistributionAllocationService(
    private val planningService: DistributionPlanningService
) extends DistributionAllocationService:

  private val findRequestSql =
    "SELECT * FROM HOUSEHOLD_REQUESTS WHERE ID = ?"

  private val findFoodItemSql =
    "SELECT * FROM FOOD_ITEMS WHERE ID = ?"

  private val allocatedForRequestSql =
    """
      |SELECT COALESCE(SUM(ALLOCATED_QUANTITY), 0)
      |FROM DISTRIBUTION_PLANS
      |WHERE HOUSEHOLD_REQUEST_ID = ?
      |  AND STATUS IN ('Planned', 'Completed')
      |""".stripMargin

  private val allocatedForFoodItemSql =
    """
      |SELECT COALESCE(SUM(ALLOCATED_QUANTITY), 0)
      |FROM DISTRIBUTION_PLANS
      |WHERE FOOD_ITEM_ID = ?
      |  AND STATUS IN ('Planned', 'Completed')
      |""".stripMargin

  private val insertPlanSql =
    """
      |INSERT INTO DISTRIBUTION_PLANS
      |  (
      |    ID,
      |    HOUSEHOLD_REQUEST_ID,
      |    HOUSEHOLD_NAME,
      |    FOOD_ITEM_ID,
      |    FOOD_ITEM_NAME,
      |    ALLOCATED_QUANTITY,
      |    ALLOCATED_UNIT,
      |    DISTRIBUTION_DATE,
      |    STATUS
      |  )
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  private val markRequestFulfilledSql =
    """
      |UPDATE HOUSEHOLD_REQUESTS
      |SET STATUS = 'Fulfilled'
      |WHERE ID = ?
      |  AND STATUS = 'Approved'
      |""".stripMargin

  override def allocate(
      householdRequestId: String,
      foodItemId: String,
      allocatedQuantity: Int,
      distributionDate: LocalDate
  ): Try[DistributionPlan] =
    DatabaseManager.withTransaction: connection =>
      for
        householdRequest <-
          findHouseholdRequest(
            connection,
            householdRequestId
          )
        foodItem <-
          findFoodItem(connection, foodItemId)
        alreadyAllocatedForRequest <-
          allocatedQuantityFor(
            connection,
            allocatedForRequestSql,
            householdRequest.id
          )
        alreadyAllocatedForFood <-
          allocatedQuantityFor(
            connection,
            allocatedForFoodItemSql,
            foodItem.id
          )
        remainingRequestedQuantity =
          householdRequest.requestedQuantity -
            alreadyAllocatedForRequest
        availableFoodQuantity =
          foodItem.quantity -
            alreadyAllocatedForFood
        distributionPlan <-
          planningService
            .createPlanWithAvailability(
              householdRequest,
              foodItem,
              allocatedQuantity,
              distributionDate,
              availableFoodQuantity,
              remainingRequestedQuantity
            )
            .left
            .map: errors =>
              new IllegalArgumentException(
                errors.mkString(" ")
              )
        savedPlan <-
          insertPlan(connection, distributionPlan)
        _ <-
          markRequestFulfilledWhenAllocated(
            connection,
            householdRequest,
            remainingRequestedQuantity,
            allocatedQuantity
          )
      yield
        savedPlan

  private def findHouseholdRequest(
      connection: Connection,
      requestId: String
  ): Either[Throwable, HouseholdRequest] =
    Try:
      val statement =
        connection.prepareStatement(findRequestSql)

      try
        statement.setString(1, requestId)

        val resultSet =
          statement.executeQuery()

        try
          if resultSet.next() then
            Some(
              HouseholdRequestMapper.fromResultSet(resultSet)
            )
          else
            None
        finally
          resultSet.close()
      finally
        statement.close()
    match
      case Success(Some(Right(householdRequest))) =>
        Right(householdRequest)

      case Success(Some(Left(message))) =>
        Left(new IllegalStateException(message))

      case Success(None) =>
        Left(
          new NoSuchElementException(
            s"Household request $requestId was not found."
          )
        )

      case Failure(exception) =>
        Left(exception)

  private def findFoodItem(
      connection: Connection,
      foodItemId: String
  ): Either[Throwable, FoodItem] =
    Try:
      val statement =
        connection.prepareStatement(findFoodItemSql)

      try
        statement.setString(1, foodItemId)

        val resultSet =
          statement.executeQuery()

        try
          if resultSet.next() then
            Some(FoodItemMapper.fromResultSet(resultSet))
          else
            None
        finally
          resultSet.close()
      finally
        statement.close()
    match
      case Success(Some(Right(foodItem))) =>
        Right(foodItem)

      case Success(Some(Left(message))) =>
        Left(new IllegalStateException(message))

      case Success(None) =>
        Left(
          new NoSuchElementException(
            s"Food item $foodItemId was not found."
          )
        )

      case Failure(exception) =>
        Left(exception)

  private def allocatedQuantityFor(
      connection: Connection,
      query: String,
      entityId: String
  ): Either[Throwable, Int] =
    Try:
      val statement =
        connection.prepareStatement(query)

      try
        statement.setString(1, entityId)

        val resultSet =
          statement.executeQuery()

        try
          if resultSet.next() then
            resultSet.getInt(1)
          else
            0
        finally
          resultSet.close()
      finally
        statement.close()
    .toEither

  private def insertPlan(
      connection: Connection,
      distributionPlan: DistributionPlan
  ): Either[Throwable, DistributionPlan] =
    Try:
      val statement =
        connection.prepareStatement(insertPlanSql)

      try
        bindPlan(statement, distributionPlan)
        statement.executeUpdate()
        distributionPlan
      finally
        statement.close()
    .toEither

  private def bindPlan(
      statement: PreparedStatement,
      distributionPlan: DistributionPlan
  ): Unit =
    statement.setString(1, distributionPlan.id)
    statement.setString(
      2,
      distributionPlan.householdRequestId
    )
    statement.setString(
      3,
      distributionPlan.householdName
    )
    statement.setString(
      4,
      distributionPlan.foodItemId
    )
    statement.setString(
      5,
      distributionPlan.foodItemName
    )
    statement.setInt(
      6,
      distributionPlan.allocatedQuantity
    )
    statement.setString(
      7,
      distributionPlan.allocatedUnit
    )
    statement.setDate(
      8,
      Date.valueOf(distributionPlan.distributionDate)
    )
    statement.setString(
      9,
      DistributionStatus.Planned.toString
    )

  private def markRequestFulfilledWhenAllocated(
      connection: Connection,
      householdRequest: HouseholdRequest,
      remainingRequestedQuantity: Int,
      allocatedQuantity: Int
  ): Either[Throwable, Unit] =
    if allocatedQuantity < remainingRequestedQuantity then
      Right(())
    else
      Try:
        val statement =
          connection.prepareStatement(
            markRequestFulfilledSql
          )

        try
          statement.setString(1, householdRequest.id)

          val affectedRows =
            statement.executeUpdate()

          if affectedRows == 1 then
            Right(())
          else
            Left(
              new IllegalStateException(
                "The household request changed before " +
                  "the allocation could be completed."
              )
            )
        finally
          statement.close()
      match
        case Success(result) =>
          result

        case Failure(exception) =>
          Left(exception)
