package foodpantry.service

import foodpantry.model.{
  ExpiryAlert,
  FoodItem,
  InventoryAlert,
  LowStockAlert
}

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ai-assisted: #19
// why: AI helped create immutable low-stock and expiry monitoring logic.
class InventoryMonitoringService(
    private val lowStockThreshold: Int = 5,
    private val expiryWarningDays: Long = 7
):

  def findAlerts(
      foodItems: List[FoodItem],
      today: LocalDate = LocalDate.now
  ): List[InventoryAlert] =
    foodItems
      .flatMap(foodItem => alertsFor(foodItem, today))
      .sortBy(alertPriority)

  private def alertsFor(
      foodItem: FoodItem,
      today: LocalDate
  ): List[InventoryAlert] =
    val lowStockAlerts =
      Option
        .when(foodItem.quantity <= lowStockThreshold)(
          LowStockAlert(foodItem, lowStockThreshold)
        )
        .toList

    val expiryAlerts =
      foodItem.expiryDate
        .filter: expiryDate =>
          !expiryDate.isAfter(
            today.plusDays(expiryWarningDays)
          )
        .map: expiryDate =>
          ExpiryAlert(
            foodItem,
            expiryDate,
            ChronoUnit.DAYS.between(today, expiryDate)
          )
        .toList

    lowStockAlerts ++ expiryAlerts

  private def alertPriority(
      inventoryAlert: InventoryAlert
  ): (Int, Long, String) =
    inventoryAlert match
      case expiryAlert: ExpiryAlert =>
        (
          0,
          expiryAlert.daysRemaining,
          expiryAlert.foodItem.name
        )

      case lowStockAlert: LowStockAlert =>
        (
          1,
          lowStockAlert.foodItem.quantity.toLong,
          lowStockAlert.foodItem.name
        )