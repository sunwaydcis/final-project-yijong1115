package foodpantry

import foodpantry.model.{
  ExpiryAlert,
  LowStockAlert,
  PerishableFood,
  ShelfStableFood
}
import foodpantry.service.InventoryDashboardSummaryService

import munit.FunSuite

import java.time.LocalDate

// ai-assisted: #53
// why: AI helped verify the new dashboard counts and attention-item deduplication.
class InventoryDashboardSummaryServiceSuite extends FunSuite:

  private val today =
    LocalDate.of(2026, 8, 4)

  private val rice =
    ShelfStableFood(
      "food-1",
      "Rice",
      "Grains",
      3,
      "packs",
      None
    )

  private val milk =
    PerishableFood(
      "food-2",
      "Milk",
      "Dairy",
      2,
      "bottles",
      today.plusDays(2)
    )

  private val beans =
    ShelfStableFood(
      "food-3",
      "Beans",
      "Canned Food",
      20,
      "cans",
      None
    )

  test("summary counts alert types and avoids double-counting attention items"):
    val alerts =
      List(
        LowStockAlert(rice, 5),
        LowStockAlert(milk, 5),
        ExpiryAlert(milk, today.plusDays(2), 2)
      )

    val summary =
      InventoryDashboardSummaryService.generate(
        List(rice, milk, beans),
        alerts
      )

    assertEquals(summary.foodRecords, 3)
    assertEquals(summary.lowStockItems, 2)
    assertEquals(summary.expiryAlertItems, 1)
    assertEquals(summary.itemsRequiringAttention, 2)

  test("summary returns zero alert counts when inventory needs no attention"):
    val summary =
      InventoryDashboardSummaryService.generate(
        List(beans),
        List.empty
      )

    assertEquals(summary.foodRecords, 1)
    assertEquals(summary.lowStockItems, 0)
    assertEquals(summary.expiryAlertItems, 0)
    assertEquals(summary.itemsRequiringAttention, 0)
