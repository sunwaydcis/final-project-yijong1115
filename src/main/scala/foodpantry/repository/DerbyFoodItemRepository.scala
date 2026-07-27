package foodpantry.repository

import foodpantry.database.DatabaseManager
import foodpantry.model.{
  FoodCategories,
  FoodItem,
  PerishableFood,
  ShelfStableFood
}
import foodpantry.service.FoodItemValidator

import java.sql.{Date, PreparedStatement, ResultSet, Types}
import scala.util.{Failure, Success, Try}

// ai-assisted: #13
// why: AI helped implement safe Derby CRUD operations for immutable FoodItem objects.
class DerbyFoodItemRepository extends Repository[FoodItem]:

  private val insertSql =
    """
      |INSERT INTO FOOD_ITEMS
      |  (ID, ITEM_TYPE, NAME, CATEGORY, QUANTITY, UNIT, EXPIRY_DATE)
      |VALUES (?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  private val updateSql =
    """
      |UPDATE FOOD_ITEMS
      |SET ITEM_TYPE = ?,
      |    NAME = ?,
      |    CATEGORY = ?,
      |    QUANTITY = ?,
      |    UNIT = ?,
      |    EXPIRY_DATE = ?
      |WHERE ID = ?
      |""".stripMargin

  private val deleteSql =
    "DELETE FROM FOOD_ITEMS WHERE ID = ?"

  private val findByIdSql =
    "SELECT * FROM FOOD_ITEMS WHERE ID = ?"

  private val findAllSql =
    "SELECT * FROM FOOD_ITEMS ORDER BY NAME"

  override def add(entity: FoodItem): Try[FoodItem] =
    validate(normalizeCategory(entity)).flatMap: validItem =>
      DatabaseManager.withConnection: connection =>
        val statement = connection.prepareStatement(insertSql)

        try
          bindInsertStatement(statement, validItem)
          statement.executeUpdate()
          validItem
        finally
          statement.close()

  override def update(entity: FoodItem): Try[FoodItem] =
    validate(normalizeCategory(entity)).flatMap: validItem =>
      DatabaseManager.withConnection: connection =>
        val statement = connection.prepareStatement(updateSql)

        try
          bindUpdateStatement(statement, validItem)

          val affectedRows = statement.executeUpdate()

          if affectedRows == 0 then
            throw new NoSuchElementException(
              s"Food item ${validItem.id} was not found."
            )

          validItem
        finally
          statement.close()

  override def delete(id: String): Try[Boolean] =
    DatabaseManager.withConnection: connection =>
      val statement = connection.prepareStatement(deleteSql)

      try
        statement.setString(1, id)
        statement.executeUpdate() > 0
      finally
        statement.close()

  override def findById(id: String): Try[Option[FoodItem]] =
    DatabaseManager.withConnection: connection =>
      val statement = connection.prepareStatement(findByIdSql)

      try
        statement.setString(1, id)

        val resultSet = statement.executeQuery()

        try
          Option.when(resultSet.next())(
            mapCurrentRow(resultSet)
          )
        finally
          resultSet.close()
      finally
        statement.close()

  override def findAll(): Try[List[FoodItem]] =
    DatabaseManager.withConnection: connection =>
      val statement = connection.prepareStatement(findAllSql)

      try
        val resultSet = statement.executeQuery()

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

  // ai-assisted: #51
  // why: AI helped ensure categories are saved consistently outside the form too.
  private def normalizeCategory(
      foodItem: FoodItem
  ): FoodItem =
    val normalizedCategory =
      FoodCategories.normalize(foodItem.category)

    foodItem match
      case perishableFood: PerishableFood =>
        perishableFood.copy(
          category = normalizedCategory
        )

      case shelfStableFood: ShelfStableFood =>
        shelfStableFood.copy(
          category = normalizedCategory
        )

  private def validate(foodItem: FoodItem): Try[FoodItem] =
    FoodItemValidator.validate(foodItem) match
      case Right(validItem) =>
        Success(validItem)

      case Left(errors) =>
        Failure(
          new IllegalArgumentException(errors.mkString(" "))
        )

  private def bindInsertStatement(
      statement: PreparedStatement,
      foodItem: FoodItem
  ): Unit =
    statement.setString(1, foodItem.id)
    bindCommonValues(statement, foodItem, 2)

  private def bindUpdateStatement(
      statement: PreparedStatement,
      foodItem: FoodItem
  ): Unit =
    bindCommonValues(statement, foodItem, 1)
    statement.setString(7, foodItem.id)

  private def bindCommonValues(
      statement: PreparedStatement,
      foodItem: FoodItem,
      startingIndex: Int
  ): Unit =
    statement.setString(startingIndex, itemType(foodItem))
    statement.setString(startingIndex + 1, foodItem.name)
    statement.setString(startingIndex + 2, foodItem.category)
    statement.setInt(startingIndex + 3, foodItem.quantity)
    statement.setString(startingIndex + 4, foodItem.unit)

    foodItem.expiryDate match
      case Some(expiryDate) =>
        statement.setDate(
          startingIndex + 5,
          Date.valueOf(expiryDate)
        )

      case None =>
        statement.setNull(startingIndex + 5, Types.DATE)

  private def itemType(foodItem: FoodItem): String =
    foodItem match
      case _: PerishableFood =>
        "PERISHABLE"

      case _: ShelfStableFood =>
        "SHELF_STABLE"

  private def mapCurrentRow(resultSet: ResultSet): FoodItem =
    FoodItemMapper.fromResultSet(resultSet) match
      case Right(foodItem) =>
        foodItem

      case Left(message) =>
        throw new IllegalStateException(message)
