package foodpantry.service

import foodpantry.model.FoodItem

// ai-assisted: #12
// why: AI helped create immutable validation that reports all input errors safely.
object FoodItemValidator:

  def validate(foodItem: FoodItem): Either[List[String], FoodItem] =
    val errors = List(
      Option.when(foodItem.id.trim.isEmpty)(
        "Food item ID is required."
      ),
      Option.when(foodItem.name.trim.isEmpty)(
        "Food item name is required."
      ),
      Option.when(foodItem.category.trim.isEmpty)(
        "Food item category is required."
      ),
      Option.when(foodItem.quantity <= 0)(
        "Quantity must be greater than zero."
      ),
      Option.when(foodItem.unit.trim.isEmpty)(
        "Food item unit is required."
      )
    ).flatten

    Either.cond(
      errors.isEmpty,
      foodItem,
      errors
    )