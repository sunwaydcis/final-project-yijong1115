package foodpantry.repository

import foodpantry.database.DatabaseManager
import foodpantry.model.DistributionPlan
import foodpantry.service.DistributionPlanValidator

import java.sql.{Date, PreparedStatement, ResultSet}
import scala.util.{Failure, Success, Try}

// ai-assisted: #34
// why: AI helped implement safe Derby CRUD operations for immutable distribution plans.
class DerbyDistributionPlanRepository
    extends Repository[DistributionPlan]:

  // Kept private so SQL details remain encapsulated in the persistence layer.
  private val insertSql =
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

  private val updateSql =
    """
      |UPDATE DISTRIBUTION_PLANS
      |SET HOUSEHOLD_REQUEST_ID = ?,
      |    HOUSEHOLD_NAME = ?,
      |    FOOD_ITEM_ID = ?,
      |    FOOD_ITEM_NAME = ?,
      |    ALLOCATED_QUANTITY = ?,
      |    ALLOCATED_UNIT = ?,
      |    DISTRIBUTION_DATE = ?,
      |    STATUS = ?
      |WHERE ID = ?
      |""".stripMargin

  private val deleteSql =
    "DELETE FROM DISTRIBUTION_PLANS WHERE ID = ?"

  private val findByIdSql =
    "SELECT * FROM DISTRIBUTION_PLANS WHERE ID = ?"

  private val findAllSql =
    """
      |SELECT *
      |FROM DISTRIBUTION_PLANS
      |ORDER BY DISTRIBUTION_DATE, HOUSEHOLD_NAME
      |""".stripMargin

  override def add(
      entity: DistributionPlan
  ): Try[DistributionPlan] =
    validate(entity).flatMap: validPlan =>
      DatabaseManager.withConnection: connection =>
        val statement =
          connection.prepareStatement(insertSql)

        try
          bindInsertStatement(statement, validPlan)
          statement.executeUpdate()
          validPlan
        finally
          statement.close()

  override def update(
      entity: DistributionPlan
  ): Try[DistributionPlan] =
    validate(entity).flatMap: validPlan =>
      DatabaseManager.withConnection: connection =>
        val statement =
          connection.prepareStatement(updateSql)

        try
          bindUpdateStatement(statement, validPlan)

          val affectedRows =
            statement.executeUpdate()

          if affectedRows == 0 then
            throw new NoSuchElementException(
              s"Distribution plan ${validPlan.id} was not found."
            )

          validPlan
        finally
          statement.close()

  override def delete(id: String): Try[Boolean] =
    DatabaseManager.withConnection: connection =>
      val statement =
        connection.prepareStatement(deleteSql)

      try
        statement.setString(1, id)
        statement.executeUpdate() > 0
      finally
        statement.close()

  override def findById(
      id: String
  ): Try[Option[DistributionPlan]] =
    DatabaseManager.withConnection: connection =>
      val statement =
        connection.prepareStatement(findByIdSql)

      try
        statement.setString(1, id)

        val resultSet =
          statement.executeQuery()

        try
          Option.when(resultSet.next())(
            mapCurrentRow(resultSet)
          )
        finally
          resultSet.close()
      finally
        statement.close()

  override def findAll(): Try[List[DistributionPlan]] =
    DatabaseManager.withConnection: connection =>
      val statement =
        connection.prepareStatement(findAllSql)

      try
        val resultSet =
          statement.executeQuery()

        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(hasNextRow => hasNextRow)
            .map(_ => mapCurrentRow(resultSet))
            .toList
        finally
          resultSet.close()
      finally
        statement.close()

  private def validate(
      distributionPlan: DistributionPlan
  ): Try[DistributionPlan] =
    DistributionPlanValidator.validate(distributionPlan) match
      case Right(validPlan) =>
        Success(validPlan)

      case Left(errors) =>
        Failure(
          new IllegalArgumentException(
            errors.mkString(" ")
          )
        )

  private def bindInsertStatement(
      statement: PreparedStatement,
      distributionPlan: DistributionPlan
  ): Unit =
    statement.setString(1, distributionPlan.id)
    bindCommonValues(statement, distributionPlan, 2)

  private def bindUpdateStatement(
      statement: PreparedStatement,
      distributionPlan: DistributionPlan
  ): Unit =
    bindCommonValues(statement, distributionPlan, 1)
    statement.setString(9, distributionPlan.id)

  private def bindCommonValues(
      statement: PreparedStatement,
      distributionPlan: DistributionPlan,
      startingIndex: Int
  ): Unit =
    statement.setString(
      startingIndex,
      distributionPlan.householdRequestId
    )

    statement.setString(
      startingIndex + 1,
      distributionPlan.householdName
    )

    statement.setString(
      startingIndex + 2,
      distributionPlan.foodItemId
    )

    statement.setString(
      startingIndex + 3,
      distributionPlan.foodItemName
    )

    statement.setInt(
      startingIndex + 4,
      distributionPlan.allocatedQuantity
    )

    statement.setString(
      startingIndex + 5,
      distributionPlan.allocatedUnit
    )

    statement.setDate(
      startingIndex + 6,
      Date.valueOf(distributionPlan.distributionDate)
    )

    statement.setString(
      startingIndex + 7,
      distributionPlan.status.toString
    )

  private def mapCurrentRow(
      resultSet: ResultSet
  ): DistributionPlan =
    DistributionPlanMapper.fromResultSet(resultSet) match
      case Right(distributionPlan) =>
        distributionPlan

      case Left(message) =>
        throw new IllegalStateException(message)