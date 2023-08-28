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

class TodoServiceTest extends AnyFunSpec {
  describe("TodoService") {
    case class MockState(rawUuid: String, nowTime: Long)
    given UUIDGen[CurriedState[MockState]] with {
      override def randomUUID: CurriedState[MockState][UUID] = State[MockState, UUID] { mockState =>
        val MockState(uuid, _) = mockState

        (mockState, UUID.fromString(uuid))
      }
    }

    given Clock[CurriedState[MockState]] with {
      override def realTime: CurriedState[MockState][FiniteDuration] = State[MockState, FiniteDuration] { mockState =>
        val MockState(_, nowTime) = mockState

        (mockState, FiniteDuration(nowTime, TimeUnit.MILLISECONDS))
      }
    }

    describe("parseAsTodo") {
      describe("success") {
        case class Input(rawUuid: String, rawTitle: String, rawDescription: String, nowTime: Long)
        case class TestCase(input: Input, expected: Right[Throwable, Todo])
        val testCaseList: List[TestCase] = List(
          TestCase(
            Input("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", "rawTitle", "rawDescription", 1627980601000L),
            Right(
              Todo(UUID.fromString("a5f9c478-01c0-4c0d-abcd-ee189b28fca1"), "rawTitle", "rawDescription", 1627980601000L, 1627980601000L)
            )
          )
        )

        testCaseList.foreach { case TestCase(input, expected) =>
          describe(s"when call with $input") {
            val Input(rawUuid, rawTitle, rawDescription, nowTime) = input
            val (_, result) = parseAsTodo[CurriedState[MockState]](rawTitle, rawDescription).value
              .run(MockState(rawUuid, nowTime))
              .value

            it(s"should return $expected") {
              assert(expected == result)
            }
          }
        }

      }
    }
  }
}
