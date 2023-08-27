package dog.shebang
package usecase.todoservice

import repository.RepositoryError
import repository.creator.Creator
import utility.typeclass.clock.Clock

import cats.Monad
import cats.data.EitherT
import cats.effect.std.UUIDGen

trait TodoService {
  def save[F[_] : Monad](using UUIDGen[F], Creator[F], Clock[F])(rawTitle: String, rawDescription: String): EitherT[F, TodoServiceError, Unit]
}
