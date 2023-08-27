package dog.shebang
package usecase.todoservice.parse.todo

import domain.todo.{Todo, TodoUtil}
import utility.typeclass.clock.Clock

import cats.Monad
import cats.syntax.functor.*
import cats.data.EitherT
import cats.effect.std.UUIDGen

private[usecase] def parseAsTodo[F[_] : Monad](using generator: UUIDGen[F], clock: Clock[F])(rawTitle: String, rawDescription: String): EitherT[F, ParseError, Todo] = for {
  title <- EitherT.fromEither[F](TodoUtil.refineTitle(rawTitle)).leftMap(_ => ParseTitleError)
  description <- EitherT.fromEither[F](TodoUtil.refineDescription(rawDescription)).leftMap(_ => ParseDescriptionError)
  id <- EitherT.right(TodoUtil.generateId)
  createdAt <- EitherT.right(clock.realTime.map(_.toMillis)).leftMap(_ => ClockError)
  updatedAt = createdAt
} yield Todo(id, title, description, createdAt, updatedAt)
