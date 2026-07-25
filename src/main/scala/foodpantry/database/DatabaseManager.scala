package foodpantry.database

import java.sql.{Connection, DriverManager}
import scala.util.Try

// ai-assisted: #9
// why: AI helped encapsulate Derby connections and ensure each connection closes safely.
object DatabaseManager:

  // Kept private so other classes cannot change the shared database location.
  private val databaseUrl =
    "jdbc:derby:data/foodPantryDB;create=true"

  def withConnection[T](operation: Connection => T): Try[T] =
    Try:
      val connection = DriverManager.getConnection(databaseUrl)

      try
        operation(connection)
      finally
        connection.close()

  def testConnection(): Try[Boolean] =
    withConnection(connection => !connection.isClosed)