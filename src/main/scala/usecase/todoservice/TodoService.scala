package dog.shebang
package usecase.todoservice

import repository.RepositoryError
import repository.creator.Creator
import utility.typeclass.clock.Clock

import cats.Monad
import cats.data.EitherT
import cats.effect.std.UUIDGen
import java.util.UUID
import dog.shebang.domain.todo.Todo
import dog.shebang.repository.reader.Reader

trait TodoService {
  def save[F[_] : Monad](using UUIDGen[F], Creator[F], Clock[F])(rawTitle: String, rawDescription: String): EitherT[F, TodoServiceError, UUID]

  def read[F[_] : Monad](using Reader[F])(id: UUID): EitherT[F, TodoServiceError, Todo]
}
