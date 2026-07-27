package foodpantry.repository

import foodpantry.database.DatabaseManager
import foodpantry.model.{
  FoodCategories,
  HouseholdRequest
}
import foodpantry.service.HouseholdRequestValidator

import java.sql.{Date, PreparedStatement, ResultSet}
import scala.util.{Failure, Success, Try}

// ai-assisted: #25
// why: AI helped implement safe Derby CRUD operations for immutable household requests.
class DerbyHouseholdRequestRepository
    extends Repository[HouseholdRequest]:

  // Kept private so SQL details remain encapsulated inside the persistence layer.
  private val insertSql =
    """
      |INSERT INTO HOUSEHOLD_REQUESTS
      |  (
      |    ID,
      |    HOUSEHOLD_NAME,
      |    HOUSEHOLD_SIZE,
      |    REQUESTED_CATEGORY,
      |    REQUESTED_QUANTITY,
      |    REQUEST_DATE,
      |    STATUS
      |  )
      |VALUES (?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  private val updateSql =
    """
      |UPDATE HOUSEHOLD_REQUESTS
      |SET HOUSEHOLD_NAME = ?,
      |    HOUSEHOLD_SIZE = ?,
      |    REQUESTED_CATEGORY = ?,
      |    REQUESTED_QUANTITY = ?,
      |    REQUEST_DATE = ?,
      |    STATUS = ?
      |WHERE ID = ?
      |""".stripMargin

  private val deleteSql =
    "DELETE FROM HOUSEHOLD_REQUESTS WHERE ID = ?"

  private val findByIdSql =
    "SELECT * FROM HOUSEHOLD_REQUESTS WHERE ID = ?"

  private val findAllSql =
    """
      |SELECT *
      |FROM HOUSEHOLD_REQUESTS
      |ORDER BY REQUEST_DATE DESC, HOUSEHOLD_NAME
      |""".stripMargin

  override def add(
      entity: HouseholdRequest
  ): Try[HouseholdRequest] =
    validate(normalizeCategory(entity)).flatMap: validRequest =>
      DatabaseManager.withConnection: connection =>
        val statement =
          connection.prepareStatement(insertSql)

        try
          bindInsertStatement(statement, validRequest)
          statement.executeUpdate()
          validRequest
        finally
          statement.close()

  override def update(
      entity: HouseholdRequest
  ): Try[HouseholdRequest] =
    validate(normalizeCategory(entity)).flatMap: validRequest =>
      DatabaseManager.withConnection: connection =>
        val statement =
          connection.prepareStatement(updateSql)

        try
          bindUpdateStatement(statement, validRequest)

          val affectedRows =
            statement.executeUpdate()

          if affectedRows == 0 then
            throw new NoSuchElementException(
              s"Household request ${validRequest.id} was not found."
            )

          validRequest
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
  ): Try[Option[HouseholdRequest]] =
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

  override def findAll(): Try[List[HouseholdRequest]] =
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

  private def normalizeCategory(
      householdRequest: HouseholdRequest
  ): HouseholdRequest =
    householdRequest.copy(
      requestedCategory =
        FoodCategories.normalize(
          householdRequest.requestedCategory
        )
    )

  private def validate(
      householdRequest: HouseholdRequest
  ): Try[HouseholdRequest] =
    HouseholdRequestValidator.validate(householdRequest) match
      case Right(validRequest) =>
        Success(validRequest)

      case Left(errors) =>
        Failure(
          new IllegalArgumentException(
            errors.mkString(" ")
          )
        )

  private def bindInsertStatement(
      statement: PreparedStatement,
      householdRequest: HouseholdRequest
  ): Unit =
    statement.setString(1, householdRequest.id)
    bindCommonValues(statement, householdRequest, 2)

  private def bindUpdateStatement(
      statement: PreparedStatement,
      householdRequest: HouseholdRequest
  ): Unit =
    bindCommonValues(statement, householdRequest, 1)
    statement.setString(7, householdRequest.id)

  private def bindCommonValues(
      statement: PreparedStatement,
      householdRequest: HouseholdRequest,
      startingIndex: Int
  ): Unit =
    statement.setString(
      startingIndex,
      householdRequest.householdName
    )

    statement.setInt(
      startingIndex + 1,
      householdRequest.householdSize
    )

    statement.setString(
      startingIndex + 2,
      householdRequest.requestedCategory
    )

    statement.setInt(
      startingIndex + 3,
      householdRequest.requestedQuantity
    )

    statement.setDate(
      startingIndex + 4,
      Date.valueOf(householdRequest.requestDate)
    )

    statement.setString(
      startingIndex + 5,
      householdRequest.status.toString
    )

  private def mapCurrentRow(
      resultSet: ResultSet
  ): HouseholdRequest =
    HouseholdRequestMapper.fromResultSet(resultSet) match
      case Right(householdRequest) =>
        householdRequest

      case Left(message) =>
        throw new IllegalStateException(message)
