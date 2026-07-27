package foodpantry.service

import foodpantry.model.{FoodItem, FoodUnits}

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
      // ai-assisted: #50
      // why: AI helped reject numeric and malformed inventory categories.
      Option.when(
        foodItem.category.trim.nonEmpty &&
          !isValidCategory(foodItem.category)
      )(
        "Category may contain letters, spaces, hyphens, / and & only."
      ),
      Option.when(foodItem.quantity <= 0)(
        "Quantity must be greater than zero."
      ),
      Option.when(foodItem.unit.trim.isEmpty)(
        "Food item unit is required."
      ),
      // ai-assisted: #49
      // why: AI helped reject numeric and unsupported units outside the UI too.
      Option.when(
        foodItem.unit.trim.nonEmpty &&
          !FoodUnits.isSupported(foodItem.unit)
      )(
        "Choose a supported food unit."
      )
    ).flatten

    Either.cond(
      errors.isEmpty,
      foodItem,
      errors
    )

  private def isValidCategory(category: String): Boolean =
    category.trim.matches(
      """[\p{L}][\p{L}\s&/-]*"""
    )
