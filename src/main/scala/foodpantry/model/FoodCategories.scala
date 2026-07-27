package foodpantry.model

// ai-assisted: #51
// why: AI helped centralise case-insensitive category formatting across the application.
object FoodCategories:

  def normalize(category: String): String =
    category
      .trim
      .split("""\s+""")
      .filter(word => word.nonEmpty)
      .map: word =>
        s"${word.head.toUpper}${word.tail.toLowerCase}"
      .mkString(" ")
