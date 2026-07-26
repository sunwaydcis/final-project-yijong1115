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
            |  STATUS VARCHAR(20) NOT NULL
            |)
            |""".stripMargin
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