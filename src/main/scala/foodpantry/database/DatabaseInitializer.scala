package foodpantry.database

import java.sql.Connection
import scala.util.Try

// ai-assisted: #10
// why: AI helped create an idempotent Derby table initializer with safe resource handling.
object DatabaseInitializer:

  // Kept private so table naming remains controlled by the persistence layer.
  private val foodItemsTableName = "FOOD_ITEMS"

  def initialize(): Try[Unit] =
    DatabaseManager.withConnection: connection =>
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

      ()

  private def tableExists(
      connection: Connection,
      tableName: String
  ): Boolean =
    val resultSet =
      connection.getMetaData.getTables(null, null, tableName, null)

    try
      resultSet.next()
    finally
      resultSet.close()