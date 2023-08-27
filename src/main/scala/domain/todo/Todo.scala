package dog.shebang
package domain.todo

import cats.effect.std.UUIDGen
import io.github.iltotore.iron.*

import java.util.UUID

case class Todo(id: UUID, title: TodoRefinement.Title, description: TodoRefinement.Description, createdAt: Long, updatedAt: Long)

object TodoUtil {
  def refineTitle(title: String): Either[String, TodoRefinement.Title] =
    title.refineEither[TodoRefinement.NonEmptyString]

  def refineDescription(description: String): Either[String, TodoRefinement.Description] =
    description.refineEither[TodoRefinement.NonEmptyString]

  def generateId[F[_]](using generator: UUIDGen[F]): F[UUID] = generator.randomUUID
}
