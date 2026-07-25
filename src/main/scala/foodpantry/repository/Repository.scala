package foodpantry.repository

import foodpantry.model.Entity
import scala.util.Try

// ai-assisted: #8
// why: AI helped define a reusable generic repository contract with safe result types.
trait Repository[T <: Entity]:

  def add(entity: T): Try[T]

  def update(entity: T): Try[T]

  def delete(id: String): Try[Boolean]

  def findById(id: String): Try[Option[T]]

  def findAll(): Try[List[T]]