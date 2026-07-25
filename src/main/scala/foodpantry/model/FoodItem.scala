package foodpantry.model

import java.time.LocalDate

// ai-assisted: #7
// why: AI helped design an immutable food-item hierarchy for subtype polymorphism.
sealed trait FoodItem extends Entity:
  def name: String
  def category: String
  def quantity: Int
  def unit: String
  def expiryDate: Option[LocalDate]
  def storageInstruction: String

case class PerishableFood(
    id: String,
    name: String,
    category: String,
    quantity: Int,
    unit: String,
    useByDate: LocalDate
) extends FoodItem:

  override def expiryDate: Option[LocalDate] =
    Some(useByDate)

  override def storageInstruction: String =
    "Keep refrigerated"

case class ShelfStableFood(
    id: String,
    name: String,
    category: String,
    quantity: Int,
    unit: String,
    bestBeforeDate: Option[LocalDate]
) extends FoodItem:

  override def expiryDate: Option[LocalDate] =
    bestBeforeDate

  override def storageInstruction: String =
    "Store in a cool, dry place"