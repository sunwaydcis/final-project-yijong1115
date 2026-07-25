package foodpantry.model

import java.time.LocalDate

// ai-assisted: #18
// why: AI helped design immutable alert subtypes for inventory monitoring.
sealed trait InventoryAlert:
  def foodItem: FoodItem
  def message: String

case class LowStockAlert(
    foodItem: FoodItem,
    minimumQuantity: Int
) extends InventoryAlert:

  override def message: String =
    s"${foodItem.name} is low in stock: " +
      s"${foodItem.quantity} ${foodItem.unit} remaining."

case class ExpiryAlert(
    foodItem: FoodItem,
    expiryDate: LocalDate,
    daysRemaining: Long
) extends InventoryAlert:

  override def message: String =
    if daysRemaining < 0 then
      s"${foodItem.name} expired on $expiryDate."
    else
      s"${foodItem.name} expires in $daysRemaining day(s)."