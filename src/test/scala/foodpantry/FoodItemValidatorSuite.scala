package foodpantry

import foodpantry.model.PerishableFood
import foodpantry.service.FoodItemValidator

import munit.FunSuite

import java.time.LocalDate

// ai-assisted: #49
// why: AI helped verify supported units and numeric-unit rejection.
class FoodItemValidatorSuite extends FunSuite:

  private def foodWithUnit(unit: String): PerishableFood =
    foodWithCategoryAndUnit("Dairy", unit)

  private def foodWithCategoryAndUnit(
      category: String,
      unit: String
  ): PerishableFood =
    PerishableFood(
      "food-1",
      "Fresh milk",
      category,
      10,
      unit,
      LocalDate.now.plusDays(5)
    )

  test("accepts a supported food unit"):
    assert(
      FoodItemValidator
        .validate(foodWithUnit("bottles"))
        .isRight
    )

  test("rejects a numeric food unit"):
    val validationResult =
      FoodItemValidator.validate(foodWithUnit("123"))

    assert(
      validationResult.left
        .exists(
          errors =>
            errors.contains("Choose a supported food unit.")
        )
    )

  test("accepts a category containing words and separators"):
    assert(
      FoodItemValidator
        .validate(
          foodWithCategoryAndUnit(
            "Canned & Dry Goods",
            "cans"
          )
        )
        .isRight
    )

  test("rejects a numeric food category"):
    val validationResult =
      FoodItemValidator.validate(
        foodWithCategoryAndUnit("123", "packs")
      )

    assert(
      validationResult.left
        .exists(
          errors =>
            errors.contains(
              "Category may contain letters, spaces, hyphens, / and & only."
            )
        )
    )
