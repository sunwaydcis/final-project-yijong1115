package foodpantry

import foodpantry.model.FoodCategories

import munit.FunSuite

// ai-assisted: #51
// why: AI helped verify that case-only category variations become one value.
class FoodCategoriesSuite extends FunSuite:

  test("normalizes category casing"):
    assertEquals(
      FoodCategories.normalize("meat"),
      "Meat"
    )

    assertEquals(
      FoodCategories.normalize("MEAT"),
      "Meat"
    )

  test("normalizes spacing and multi-word categories"):
    assertEquals(
      FoodCategories.normalize("  canned   GOODS  "),
      "Canned Goods"
    )
