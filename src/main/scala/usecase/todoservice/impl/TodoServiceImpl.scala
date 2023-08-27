package dog.shebang
package usecase.todoservice.impl

import domain.todo.{Todo, TodoUtil}
import repository.*
import repository.creator.*
import repository.reader.{ReadError, Reader}
import usecase.todoservice.parse.todo.parseAsTodo
import usecase.todoservice.{TodoRepositoryError, TodoService, TodoServiceError}
import utility.typeclass.clock.Clock
import utility.typeconstructor.CurriedState

import cats.Monad
import cats.data.{EitherT, State}
import cats.effect.std.UUIDGen
import cats.effect.{IO, std, Clock as CatsClock}
import cats.syntax.functor.*
import io.github.iltotore.iron.autoRefine
import org.scalatest.funspec.AnyFunSpec

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.collection.mutable.Stack
import scala.concurrent.duration.FiniteDuration

object TodoService extends TodoService {
  private def liftToTodoRepositoryError(createError: CreateError): TodoServiceError =
    CreateException.apply.andThen(TodoRepositoryError.apply)(createError)

  override def save[F[_] : Monad](using generator: UUIDGen[F], creator: Creator[F], clock: Clock[F])(rawTitle: String, rawDescription: String): EitherT[F, TodoServiceError, Unit] = {
    val parseEither = parseAsTodo[F](rawTitle, rawDescription)
    val liftedExceptionEither = parseEither.leftMap(ParseException.apply.andThen(liftToTodoRepositoryError))

    liftedExceptionEither.flatMap(creator.create.andThen(_.leftMap(liftToTodoRepositoryError)))
  }
}
