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
import dog.shebang.repository.updator.Updator
import dog.shebang.repository.deletor.Deletor
import dog.shebang.domain.todo.TodoRefinement

trait TodoService {
  def save[F[_] : Monad](using UUIDGen[F], Creator[F], Clock[F])(rawTitle: String, rawDescription: String): EitherT[F, TodoServiceError, UUID]

  def read[F[_] : Monad](using Reader[F])(id: String): EitherT[F, TodoServiceError, Todo]

  def readAll[F[_] : Monad](using Reader[F])(): EitherT[F, TodoServiceError, List[Todo]]

  def update[F[_] : Monad](using Updator[F])(id: String, title: String, description: String): EitherT[F, TodoServiceError, UUID]

  def delete[F[_] : Monad](using Deletor[F])(id: String): EitherT[F, TodoServiceError, UUID]
}
