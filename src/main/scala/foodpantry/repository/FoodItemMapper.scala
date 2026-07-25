package foodpantry.repository

import foodpantry.model.{FoodItem, PerishableFood, ShelfStableFood}

import java.sql.ResultSet

// ai-assisted: #11
// why: AI helped map Derby rows safely into the correct FoodItem subtype.
object FoodItemMapper:

  def fromResultSet(resultSet: ResultSet): Either[String, FoodItem] =
    val id = resultSet.getString("ID")
    val name = resultSet.getString("NAME")
    val category = resultSet.getString("CATEGORY")
    val quantity = resultSet.getInt("QUANTITY")
    val unit = resultSet.getString("UNIT")

    val expiryDate =
      Option(resultSet.getDate("EXPIRY_DATE"))
        .map(databaseDate => databaseDate.toLocalDate)

    resultSet.getString("ITEM_TYPE") match
      case "PERISHABLE" =>
        expiryDate
          .toRight(s"Perishable food $id requires an expiry date.")
          .map: useByDate =>
            PerishableFood(
              id,
              name,
              category,
              quantity,
              unit,
              useByDate
            )

      case "SHELF_STABLE" =>
        Right(
          ShelfStableFood(
            id,
            name,
            category,
            quantity,
            unit,
            expiryDate
          )
        )

      case unsupportedType =>
        Left(s"Unsupported food item type: $unsupportedType")
