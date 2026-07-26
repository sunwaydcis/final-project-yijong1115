package foodpantry.database

import java.sql.{Connection, DriverManager}
import scala.util.{Failure, Success, Try, Using}

// ai-assisted: #9
// why: AI helped encapsulate Derby connections and ensure each connection closes safely.
object DatabaseManager:

  // Kept private so production code cannot change the shared database location.
  // Tests may provide an isolated location through a JVM property.
  private def databaseUrl: String =
    Option(
      System.getProperty("foodpantry.database.url")
    ).filter(url => url.trim.nonEmpty)
      .getOrElse(
        "jdbc:derby:data/foodPantryDB;create=true"
      )

  def withConnection[T](operation: Connection => T): Try[T] =
    Try:
      val connection = DriverManager.getConnection(databaseUrl)

      try
        operation(connection)
      finally
        connection.close()

  // ai-assisted: #44
  // why: AI helped add one atomic transaction boundary for distribution allocation.
  def withTransaction[T](
      operation: Connection => Either[Throwable, T]
  ): Try[T] =
    Using(DriverManager.getConnection(databaseUrl)): connection =>
      connection.setTransactionIsolation(
        Connection.TRANSACTION_SERIALIZABLE
      )
      connection.setAutoCommit(false)

      val operationResult =
        Try(operation(connection)) match
          case Success(result) =>
            result

          case Failure(exception) =>
            Left(exception)

      operationResult match
        case Right(value) =>
          Try(connection.commit()) match
            case Success(_) =>
              value

            case Failure(exception) =>
              Try(connection.rollback())
                .failed
                .foreach(exception.addSuppressed)
              throw exception

        case Left(exception) =>
          Try(connection.rollback())
            .failed
            .foreach(exception.addSuppressed)
          throw exception

  def testConnection(): Try[Boolean] =
    withConnection(connection => !connection.isClosed)
