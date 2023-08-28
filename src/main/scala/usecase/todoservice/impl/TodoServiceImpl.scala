package dog.shebang
package usecase.todoservice.impl

import domain.todo.{Todo, TodoRefinement}
import repository.*
import repository.creator.*
import usecase.todoservice.parse.todo.{ParseDescriptionError, ParseError, ParseTitleError, parseAsTodo}
import usecase.todoservice.{TodoRepositoryError, TodoService, TodoServiceError}
import utility.typeclass.clock.Clock
import utility.typeconstructor.CurriedState

import cats.Monad
import cats.data.{EitherT, State}
import cats.effect.std
import cats.effect.std.UUIDGen
import cats.syntax.functor.*
import io.github.iltotore.iron.autoRefine
import org.scalatest.funspec.AnyFunSpec

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.collection.mutable
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
      case class TestCase[T[_, _]](input: Input, expected: T[ParseError, Todo])
      case class Input(rawUuid: String, rawTitle: String, rawDescription: String, nowTime: Long)

      val failureTestCaseList: List[TestCase[Left]] = List(
        TestCase(Input("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", "", "rawDescription", 1627980601000L), Left[ParseError, Todo](ParseTitleError)),
        TestCase(Input("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", "rawTitle", "", 1627980601000L), Left[ParseError, Todo](ParseDescriptionError)),
      )

      val successTestCaseList: List[TestCase[Right]] = List(
        TestCase(
          Input("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", "rawTitle", "rawDescription", 1627980601000L),
          Right(
            Todo(UUID.fromString("a5f9c478-01c0-4c0d-abcd-ee189b28fca1"), "rawTitle", "rawDescription", 1627980601000L, 1627980601000L)
          )
        )
      )

      val testCaseList: List[TestCase[Right] | TestCase[Left]] = successTestCaseList ++ failureTestCaseList

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

    describe("save") {
      given Creator[CurriedState[MockState]] with {
        override def create(todo: Todo): EitherT[CurriedState[MockState], CreateError, Unit] = EitherT.right(State[MockState, Unit] { mockState =>
          (mockState, ())
        })
      }

      describe("failure") {
        case class FailureInput(title: String, description: String)

        case class FailureTestCase(input: FailureInput, expected: Left[TodoServiceError, Todo])

        val failureTestCaseList: List[FailureTestCase] = List(
          FailureTestCase(FailureInput("", "rawDescription"), Left(TodoRepositoryError(CreateException(ParseException(ParseTitleError))))),
          FailureTestCase(FailureInput("rawTitle", ""), Left(TodoRepositoryError(CreateException(ParseException(ParseDescriptionError))))),
        )

        failureTestCaseList.foreach { case FailureTestCase(input, expected) =>
          describe(s"when call with $input") {
            val FailureInput(title, description) = input
            val (_, result) = TodoService.save[CurriedState[MockState]](title, description).value
              .run(MockState("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", 0))
              .value

            it(s"should return $expected") {
              assert(expected == result)
            }
          }
        }
      }

      describe("success") {
        case class SuccessInput(title: TodoRefinement.Title, description: TodoRefinement.Description)

        case class SuccessTestCase(input: SuccessInput, expected: Right[TodoServiceError, Todo])

        val successTestCaseList: List[SuccessTestCase] = List(
          SuccessTestCase(
            SuccessInput("rawTitle", "rawDescription"),
            Right(Todo(UUID.fromString("a5f9c478-01c0-4c0d-abcd-ee189b28fca1"), "rawTitle", "rawDescription", 0, 0))
          )
        )

        successTestCaseList.foreach { case SuccessTestCase(input, expected) =>
          describe(s"when call with $input") {
            val SuccessInput(title, description) = input
            val (argument, _) = TodoService.save[CurriedState[MockState]](title, description).value
              .run(MockState("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", 0))
              .value

            val MockState(rawUuid, nowTime) = argument
            val result = Right(Todo(UUID.fromString(rawUuid), title, description, nowTime, nowTime))

            it(s"should return $expected") {
              assert(expected == result)
            }
          }

        }
      }
    }
  }

}
