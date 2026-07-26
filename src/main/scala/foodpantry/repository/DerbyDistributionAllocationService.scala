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

  private val findDistributionPlanSql =
    "SELECT * FROM DISTRIBUTION_PLANS WHERE ID = ?"

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
      |  AND STATUS = 'Planned'
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

  private val completedForRequestSql =
    """
      |SELECT COALESCE(SUM(ALLOCATED_QUANTITY), 0)
      |FROM DISTRIBUTION_PLANS
      |WHERE HOUSEHOLD_REQUEST_ID = ?
      |  AND STATUS = 'Completed'
      |""".stripMargin

  private val decrementFoodItemSql =
    """
      |UPDATE FOOD_ITEMS
      |SET QUANTITY = QUANTITY - ?
      |WHERE ID = ?
      |  AND QUANTITY >= ?
      |""".stripMargin

  private val updatePlanStatusSql =
    """
      |UPDATE DISTRIBUTION_PLANS
      |SET STATUS = ?,
      |    STOCK_DEDUCTED =
      |      CASE WHEN ? = 'Completed' THEN 1
      |           ELSE STOCK_DEDUCTED
      |      END
      |WHERE ID = ?
      |  AND STATUS = 'Planned'
      |""".stripMargin

  private val updateRequestStatusSql =
    """
      |UPDATE HOUSEHOLD_REQUESTS
      |SET STATUS = ?
      |WHERE ID = ?
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
      yield
        savedPlan

  // ai-assisted: #46
  // why: AI helped make completion reduce stock and update related statuses atomically.
  override def complete(
      distributionPlanId: String
  ): Try[DistributionPlan] =
    DatabaseManager.withTransaction: connection =>
      for
        distributionPlan <-
          findDistributionPlan(
            connection,
            distributionPlanId
          )
        _ <- requirePlanned(distributionPlan)
        _ <- decrementInventory(
          connection,
          distributionPlan
        )
        completedPlan <- updatePlanStatus(
          connection,
          distributionPlan,
          DistributionStatus.Completed
        )
        _ <- synchroniseRequestStatus(
          connection,
          completedPlan.householdRequestId
        )
      yield
        completedPlan

  override def cancel(
      distributionPlanId: String
  ): Try[DistributionPlan] =
    DatabaseManager.withTransaction: connection =>
      for
        distributionPlan <-
          findDistributionPlan(
            connection,
            distributionPlanId
          )
        _ <- requirePlanned(distributionPlan)
        cancelledPlan <- updatePlanStatus(
          connection,
          distributionPlan,
          DistributionStatus.Cancelled
        )
        _ <- synchroniseRequestStatus(
          connection,
          cancelledPlan.householdRequestId
        )
      yield
        cancelledPlan

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

  private def findDistributionPlan(
      connection: Connection,
      distributionPlanId: String
  ): Either[Throwable, DistributionPlan] =
    Try:
      val statement =
        connection.prepareStatement(findDistributionPlanSql)

      try
        statement.setString(1, distributionPlanId)

        val resultSet =
          statement.executeQuery()

        try
          if resultSet.next() then
            Some(
              DistributionPlanMapper.fromResultSet(resultSet)
            )
          else
            None
        finally
          resultSet.close()
      finally
        statement.close()
    match
      case Success(Some(Right(distributionPlan))) =>
        Right(distributionPlan)

      case Success(Some(Left(message))) =>
        Left(new IllegalStateException(message))

      case Success(None) =>
        Left(
          new NoSuchElementException(
            s"Distribution plan $distributionPlanId was not found."
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

  private def requirePlanned(
      distributionPlan: DistributionPlan
  ): Either[Throwable, Unit] =
    Either.cond(
      distributionPlan.status == DistributionStatus.Planned,
      (),
      new IllegalStateException(
        s"Only a Planned distribution can be changed. " +
          s"This plan is already ${distributionPlan.status}."
      )
    )

  private def decrementInventory(
      connection: Connection,
      distributionPlan: DistributionPlan
  ): Either[Throwable, Unit] =
    Try:
      val statement =
        connection.prepareStatement(decrementFoodItemSql)

      try
        statement.setInt(
          1,
          distributionPlan.allocatedQuantity
        )
        statement.setString(
          2,
          distributionPlan.foodItemId
        )
        statement.setInt(
          3,
          distributionPlan.allocatedQuantity
        )

        Either.cond(
          statement.executeUpdate() == 1,
          (),
          new IllegalStateException(
            "The food item no longer has enough stock " +
              "to complete this distribution."
          )
        )
      finally
        statement.close()
    match
      case Success(result) =>
        result

      case Failure(exception) =>
        Left(exception)

  private def updatePlanStatus(
      connection: Connection,
      distributionPlan: DistributionPlan,
      newStatus: DistributionStatus
  ): Either[Throwable, DistributionPlan] =
    Try:
      val statement =
        connection.prepareStatement(updatePlanStatusSql)

      try
        statement.setString(1, newStatus.toString)
        statement.setString(2, newStatus.toString)
        statement.setString(3, distributionPlan.id)

        Either.cond(
          statement.executeUpdate() == 1,
          distributionPlan.copy(status = newStatus),
          new IllegalStateException(
            "The distribution plan changed before " +
              "the action could be completed."
          )
        )
      finally
        statement.close()
    match
      case Success(result) =>
        result

      case Failure(exception) =>
        Left(exception)

  private def synchroniseRequestStatus(
      connection: Connection,
      householdRequestId: String
  ): Either[Throwable, Unit] =
    for
      householdRequest <-
        findHouseholdRequest(
          connection,
          householdRequestId
        )
      completedQuantity <-
        allocatedQuantityFor(
          connection,
          completedForRequestSql,
          householdRequestId
        )
      requestStatus =
        if completedQuantity >=
            householdRequest.requestedQuantity
        then
          foodpantry.model.RequestStatus.Fulfilled
        else
          foodpantry.model.RequestStatus.Approved
      _ <- updateRequestStatus(
        connection,
        householdRequestId,
        requestStatus.toString
      )
    yield
      ()

  private def updateRequestStatus(
      connection: Connection,
      householdRequestId: String,
      statusValue: String
  ): Either[Throwable, Unit] =
    Try:
      val statement =
        connection.prepareStatement(updateRequestStatusSql)

      try
        statement.setString(1, statusValue)
        statement.setString(2, householdRequestId)

        Either.cond(
          statement.executeUpdate() == 1,
          (),
          new IllegalStateException(
            "The linked household request was not found."
          )
        )
      finally
        statement.close()
    match
      case Success(result) =>
        result

      case Failure(exception) =>
        Left(exception)
