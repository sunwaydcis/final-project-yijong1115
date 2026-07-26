package foodpantry.database

import java.sql.Connection
import scala.util.Try

// ai-assisted: #10
// why: AI helped create an idempotent Derby table initializer with safe resource handling.
object DatabaseInitializer:

  // Kept private so table naming remains controlled by the persistence layer.
  private val foodItemsTableName =
    "FOOD_ITEMS"

  private val householdRequestsTableName =
    "HOUSEHOLD_REQUESTS"

  private val distributionPlansTableName =
    "DISTRIBUTION_PLANS"

  def initialize(): Try[Unit] =
    DatabaseManager.withConnection: connection =>
      createFoodItemsTable(connection)
      createHouseholdRequestsTable(connection)
      createDistributionPlansTable(connection)
      ensureStockDeductedColumn(connection)
      migrateCompletedDistributionStock(connection)

  private def createFoodItemsTable(
      connection: Connection
  ): Unit =
    if !tableExists(connection, foodItemsTableName) then
      val statement = connection.createStatement()

      try
        statement.executeUpdate(
          """
            |CREATE TABLE FOOD_ITEMS (
            |  ID VARCHAR(36) PRIMARY KEY,
            |  ITEM_TYPE VARCHAR(30) NOT NULL,
            |  NAME VARCHAR(100) NOT NULL,
            |  CATEGORY VARCHAR(50) NOT NULL,
            |  QUANTITY INT NOT NULL,
            |  UNIT VARCHAR(30) NOT NULL,
            |  EXPIRY_DATE DATE
            |)
            |""".stripMargin
        )
      finally
        statement.close()

  // ai-assisted: #22
  // why: AI helped extend the initializer with persistent household requests.
  private def createHouseholdRequestsTable(
      connection: Connection
  ): Unit =
    if !tableExists(
        connection,
        householdRequestsTableName
      )
    then
      val statement = connection.createStatement()

      try
        statement.executeUpdate(
          """
            |CREATE TABLE HOUSEHOLD_REQUESTS (
            |  ID VARCHAR(36) PRIMARY KEY,
            |  HOUSEHOLD_NAME VARCHAR(100) NOT NULL,
            |  HOUSEHOLD_SIZE INT NOT NULL,
            |  REQUESTED_CATEGORY VARCHAR(50) NOT NULL,
            |  REQUESTED_QUANTITY INT NOT NULL,
            |  REQUEST_DATE DATE NOT NULL,
            |  STATUS VARCHAR(20) NOT NULL
            |)
            |""".stripMargin
        )
      finally
        statement.close()

  // ai-assisted: #31
  // why: AI helped extend the initializer with persistent distribution plans.
  private def createDistributionPlansTable(
      connection: Connection
  ): Unit =
    if !tableExists(
        connection,
        distributionPlansTableName
      )
    then
      val statement = connection.createStatement()

      try
        statement.executeUpdate(
          """
            |CREATE TABLE DISTRIBUTION_PLANS (
            |  ID VARCHAR(36) PRIMARY KEY,
            |  HOUSEHOLD_REQUEST_ID VARCHAR(36) NOT NULL,
            |  HOUSEHOLD_NAME VARCHAR(100) NOT NULL,
            |  FOOD_ITEM_ID VARCHAR(36) NOT NULL,
            |  FOOD_ITEM_NAME VARCHAR(100) NOT NULL,
            |  ALLOCATED_QUANTITY INT NOT NULL,
            |  ALLOCATED_UNIT VARCHAR(30) NOT NULL,
            |  DISTRIBUTION_DATE DATE NOT NULL,
            |  STATUS VARCHAR(20) NOT NULL,
            |  STOCK_DEDUCTED SMALLINT DEFAULT 0 NOT NULL
            |)
            |""".stripMargin
        )
      finally
        statement.close()

  // ai-assisted: #46
  // why: AI helped make legacy completed plans reduce stock once without double deduction.
  private def ensureStockDeductedColumn(
      connection: Connection
  ): Unit =
    if !columnExists(
        connection,
        distributionPlansTableName,
        "STOCK_DEDUCTED"
      )
    then
      val statement = connection.createStatement()

      try
        statement.executeUpdate(
          """
            |ALTER TABLE DISTRIBUTION_PLANS
            |ADD COLUMN STOCK_DEDUCTED
            |SMALLINT DEFAULT 0 NOT NULL
            |""".stripMargin
        )
      finally
        statement.close()

  private def migrateCompletedDistributionStock(
      connection: Connection
  ): Unit =
    val legacyDistributions =
      loadLegacyCompletedDistributions(connection)

    if legacyDistributions.nonEmpty then
      val originalAutoCommit =
        connection.getAutoCommit

      connection.setAutoCommit(false)

      try
        legacyDistributions.foreach:
          case (
                distributionPlanId,
                foodItemId,
                allocatedQuantity
              ) =>
            deductLegacyStock(
              connection,
              foodItemId,
              allocatedQuantity
            )

            markStockDeducted(
              connection,
              distributionPlanId
            )

        connection.commit()
      catch
        case exception: Throwable =>
          connection.rollback()
          throw exception
      finally
        connection.setAutoCommit(originalAutoCommit)

  private def loadLegacyCompletedDistributions(
      connection: Connection
  ): List[(String, String, Int)] =
    val statement =
      connection.prepareStatement(
        """
          |SELECT ID, FOOD_ITEM_ID, ALLOCATED_QUANTITY
          |FROM DISTRIBUTION_PLANS
          |WHERE STATUS = 'Completed'
          |  AND STOCK_DEDUCTED = 0
          |""".stripMargin
      )

    try
      val resultSet =
        statement.executeQuery()

      try
        Iterator
          .continually(resultSet.next())
          .takeWhile(hasNextRow => hasNextRow)
          .map: _ =>
            (
              resultSet.getString("ID"),
              resultSet.getString("FOOD_ITEM_ID"),
              resultSet.getInt("ALLOCATED_QUANTITY")
            )
          .toList
      finally
        resultSet.close()
    finally
      statement.close()

  private def deductLegacyStock(
      connection: Connection,
      foodItemId: String,
      allocatedQuantity: Int
  ): Unit =
    val statement =
      connection.prepareStatement(
        """
          |UPDATE FOOD_ITEMS
          |SET QUANTITY = QUANTITY - ?
          |WHERE ID = ?
          |  AND QUANTITY >= ?
          |""".stripMargin
      )

    try
      statement.setInt(1, allocatedQuantity)
      statement.setString(2, foodItemId)
      statement.setInt(3, allocatedQuantity)

      if statement.executeUpdate() != 1 then
        throw new IllegalStateException(
          "A completed distribution could not be reconciled " +
            s"with inventory item $foodItemId."
        )
    finally
      statement.close()

  private def markStockDeducted(
      connection: Connection,
      distributionPlanId: String
  ): Unit =
    val statement =
      connection.prepareStatement(
        """
          |UPDATE DISTRIBUTION_PLANS
          |SET STOCK_DEDUCTED = 1
          |WHERE ID = ?
          |""".stripMargin
      )

    try
      statement.setString(1, distributionPlanId)

      if statement.executeUpdate() != 1 then
        throw new IllegalStateException(
          s"Distribution plan $distributionPlanId could not be reconciled."
        )
    finally
      statement.close()

  private def tableExists(
      connection: Connection,
      tableName: String
  ): Boolean =
    val resultSet =
      connection
        .getMetaData
        .getTables(null, null, tableName, null)

    try
      resultSet.next()
    finally
      resultSet.close()

  private def columnExists(
      connection: Connection,
      tableName: String,
      columnName: String
  ): Boolean =
    val resultSet =
      connection
        .getMetaData
        .getColumns(
          null,
          null,
          tableName,
          columnName
        )

    try
      resultSet.next()
    finally
      resultSet.close()
