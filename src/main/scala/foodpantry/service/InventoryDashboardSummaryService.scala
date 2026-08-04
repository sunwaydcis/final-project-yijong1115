package foodpantry.service

import foodpantry.model.{
  ExpiryAlert,
  FoodItem,
  InventoryAlert,
  LowStockAlert
}

case class InventoryDashboardSummary(
    foodRecords: Int,
    lowStockItems: Int,
    expiryAlertItems: Int,
    itemsRequiringAttention: Int
)

// ai-assisted: #53
// why: AI helped replace a mixed-unit total with meaningful, testable inventory counts.
object InventoryDashboardSummaryService:

  def generate(
      foodItems: List[FoodItem],
      inventoryAlerts: List[InventoryAlert]
  ): InventoryDashboardSummary =
    val lowStockItemIds =
      inventoryAlerts.collect:
        case lowStockAlert: LowStockAlert =>
          lowStockAlert.foodItem.id

    val expiryAlertItemIds =
      inventoryAlerts.collect:
        case expiryAlert: ExpiryAlert =>
          expiryAlert.foodItem.id

    val attentionItemIds =
      inventoryAlerts.map(
        inventoryAlert => inventoryAlert.foodItem.id
      )

    InventoryDashboardSummary(
      foodRecords = foodItems.size,
      lowStockItems = lowStockItemIds.distinct.size,
      expiryAlertItems = expiryAlertItemIds.distinct.size,
      itemsRequiringAttention =
        attentionItemIds.distinct.size
    )
