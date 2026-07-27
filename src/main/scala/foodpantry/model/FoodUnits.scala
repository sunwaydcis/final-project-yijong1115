package foodpantry.model

// ai-assisted: #49
// why: AI helped centralise valid stock units so the form and validator stay consistent.
object FoodUnits:

  val choices: List[String] =
    List(
      "bags",
      "bottles",
      "boxes",
      "cans",
      "cartons",
      "jars",
      "kg",
      "litres",
      "packs",
      "pieces",
      "trays"
    )

  private val acceptedUnits: Set[String] =
    choices.toSet ++
      Set(
        "bag",
        "bottle",
        "box",
        "can",
        "carton",
        "jar",
        "kilogram",
        "kilograms",
        "litre",
        "liter",
        "liters",
        "pack",
        "piece",
        "tray"
      )

  def isSupported(unit: String): Boolean =
    acceptedUnits.contains(unit.trim.toLowerCase)
