package dog.shebang
package repository.creator

import domain.todo.Todo

import cats.data.EitherT
import java.util.UUID

trait Creator[F[_]] {
  def create(todo: Todo): EitherT[F, CreateError, UUID]
}
