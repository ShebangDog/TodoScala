package dog.shebang
package usecase.todoservice.impl

import domain.todo.{Todo, TodoRefinement}
import repository.*
import repository.creator.*
import usecase.todoservice.generate.todo.{ParseDescriptionError, GenerateError, ParseTitleError, generateTodo}
import usecase.todoservice.{TodoRepositoryError, TodoService, TodoServiceError, UUIDParseError}
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
import dog.shebang.repository.reader.{Reader, ReadError}
import java.{util => ju}
import hedgehog.Gen
import dog.shebang.fake.MockState
import org.scalatools.testing.Result
import hedgehog.core.GenT
import dog.shebang.fake.createFakeInmemoryRepository
import generator.todo.TodoGenerator
import generator.uuid.UUIDGenerator
import dog.shebang.repository.updator.{Updator, UpdateError}
import dog.shebang.repository.deletor.{Deletor, DeleteError}
import dog.shebang.domain.todo.TodoUtil
import scala.util.Try

object TodoService extends TodoService {
  private def liftToTodoRepositoryError(createError: CreateError): TodoServiceError =
    CreateException.apply.andThen(TodoRepositoryError.apply)(createError)

  private def liftToTodoRepositoryError(readError: ReadError): TodoServiceError =
    ReadException.apply.andThen(TodoRepositoryError.apply)(readError)

  private def liftToTodoRepositoryError(updateError: UpdateError): TodoServiceError =
    UpdateException.apply.andThen(TodoRepositoryError.apply)(updateError)

  private def liftToTodoRepositoryError(deleteError: DeleteError): TodoServiceError = {
    DeleteException.apply.andThen(TodoRepositoryError.apply)(deleteError)
  }

  private def parseUUID(id: String): Either[Throwable, UUID] = Try(UUID.fromString(id)).toEither

  override def save[F[_] : Monad](using generator: UUIDGen[F], creator: Creator[F], clock: Clock[F])(rawTitle: String, rawDescription: String): EitherT[F, TodoServiceError, UUID] = {
    val parseEither = generateTodo[F](rawTitle, rawDescription)
    val liftedExceptionEither = parseEither.leftMap(ParseException.apply.andThen(liftToTodoRepositoryError))

    liftedExceptionEither.flatMap(creator.create.andThen(_.leftMap(liftToTodoRepositoryError)))
  }

  override def read[F[_] : Monad](using reader: Reader[F])(id: String): EitherT[F, TodoServiceError, Todo] = 
    for {
      uuid <- EitherT.fromEither(parseUUID(id)).leftMap(UUIDParseError.apply)
      todo <- reader.read(uuid).leftMap(liftToTodoRepositoryError)
    } yield todo
  end read

  override def readAll[F[_] : Monad](using reader: Reader[F])(): EitherT[F, TodoServiceError, List[Todo]] = 
    val todosEither = reader.readAll()

    todosEither.leftMap(liftToTodoRepositoryError)
  end readAll

  override def update[F[_]: Monad](using updator: Updator[F])(
    id: String,
    rawTitle: String,
    rawDescription: String
  ): EitherT[F, TodoServiceError, UUID] = for {
      uuid <- EitherT.fromEither(parseUUID(id)).leftMap(UUIDParseError.apply)
      title <- EitherT.fromEither[F](TodoUtil.refineTitle(rawTitle).left.map(error => liftToTodoRepositoryError(UpdateError.ParseException(error))))
      description <- EitherT.fromEither[F](TodoUtil.refineDescription(rawDescription).left.map(error => liftToTodoRepositoryError(UpdateError.ParseException(error))))
      result <- updator.update(uuid, title, description).leftMap(liftToTodoRepositoryError)
    } yield result
  end update

  override def delete[F[_]: Monad](using deletor: Deletor[F])(id: String): EitherT[F, TodoServiceError, UUID] = 
    for {
      uuid <- EitherT.fromEither(parseUUID(id)).leftMap(UUIDParseError.apply)
      result <- deletor.delete(uuid).leftMap(liftToTodoRepositoryError)
    } yield result
  end delete
}

class TodoServiceTest extends AnyFunSpec {
  describe("TodoService") {
    import dog.shebang.fake.{FakeUUIDGen, FakeClock, MockState}

    describe("parseAsTodo") {
      case class TestCase[T[_, _]](input: Input, expected: T[GenerateError, Todo])
      case class Input(rawUuid: String, rawTitle: String, rawDescription: String, nowTime: Long)

      val failureTestCaseList: List[TestCase[Left]] = List(
        TestCase(Input("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", "", "rawDescription", 1627980601000L), Left[GenerateError, Todo](ParseTitleError)),
        TestCase(Input("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", "rawTitle", "", 1627980601000L), Left[GenerateError, Todo](ParseDescriptionError)),
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
          val (_, result) = generateTodo[CurriedState[MockState]](rawTitle, rawDescription).value
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
        override def create(todo: Todo): EitherT[CurriedState[MockState], CreateError, UUID] = EitherT.right(State[MockState, UUID] { mockState =>
          (mockState, UUID.fromString("a5f9c478-01c0-4c0d-abcd-ee189b28fca1"))
        })
      }

      describe("failure") {
        case class FailureInput(title: String, description: String)

        case class FailureTestCase(input: FailureInput, expected: Left[TodoServiceError, UUID])

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

        case class SuccessTestCase(input: SuccessInput, expected: Right[TodoServiceError, UUID])

        val successTestCaseList: List[SuccessTestCase] = List(
          SuccessTestCase(
            SuccessInput("rawTitle", "rawDescription"),
            Right(UUID.fromString("a5f9c478-01c0-4c0d-abcd-ee189b28fca1"))
          )
        )

        successTestCaseList.foreach { case SuccessTestCase(input, expected) =>
          describe(s"when call with $input") {
            val SuccessInput(title, description) = input
            val (argument, _) = TodoService.save[CurriedState[MockState]](title, description).value
              .run(MockState("a5f9c478-01c0-4c0d-abcd-ee189b28fca1", 0))
              .value

            val MockState(rawUuid, nowTime) = argument
            val result = Right(UUID.fromString(rawUuid))

            it(s"should return $expected") {
              assert(expected == result)
            }
          }
        }
      }
    }
  }

}

import hedgehog.runner.{Test, Properties, property}
import hedgehog.{Property, Range, Syntax, Result as HHResult}
import usecase.todoservice.generate.todo.{GenerateError, ParseDescriptionError, generateTodo, ParseTitleError}

object TodoServiceProperty extends Properties:
  override def tests: List[Test] = 
    readTests
  end tests

  def readTests: List[Test] = 
    List(
      property("synmetry", ReadProperties.readSymmetry),
      property("noCommutativity", ReadProperties.readNoCommutativity),
      property("invariants", ReadProperties.readInvariants),
      property("idempotence", ReadProperties.readIdempotence),
    )
  end readTests

  object ReadProperties:
    import dog.shebang.fake.{FakeUUIDGen, FakeClock, createFakeInmemoryRepository}

    def readSymmetry: Property =
      given Repository[CurriedState[MockState]] = createFakeInmemoryRepository

      val result = for {
        generatedTitle <- TodoGenerator.generateRawTitle.forAll
        generatedDescription <- TodoGenerator.generateRawDescription.forAll
        timeLong <- TodoGenerator.generateTime.forAll
        generatedId <- UUIDGenerator.generateUUID.forAll
      } yield for {
        id <- TodoService.save(generatedTitle, generatedDescription).value.run(MockState(generatedId, timeLong)).value._2
        todo <- TodoService.read(id.toString()).value.run(MockState(generatedId, timeLong)).value._2
      } yield (todo.title, generatedTitle)

      result.map {
        case Right(todoTitle, generatedTitle) => todoTitle ==== generatedTitle
        case Left(value) => HHResult.failure.log("Failed to read")
      }
    end readSymmetry

    def readNoCommutativity: Property =
      given Repository[CurriedState[MockState]] = createFakeInmemoryRepository

      for {
        generatedTitle <- TodoGenerator.generateRawTitle.forAll
        generatedDescription <- TodoGenerator.generateRawDescription.forAll
        timeLong <- TodoGenerator.generateTime.forAll
        generatedId <- UUIDGenerator.generateUUID.forAll
      } yield {
        val todoBeforeSave = for {
          todo <- TodoService.read(generatedId).value.run(MockState(generatedId, timeLong)).value._2
        } yield todo

        val todoAfterSave = for {
          _ <- TodoService.save(generatedTitle, generatedDescription).value.run(MockState(generatedId, timeLong)).value._2
          todo <- TodoService.read(generatedId).value.run(MockState(generatedId, timeLong)).value._2
        } yield todo

        HHResult.diff(todoBeforeSave, todoAfterSave)(_ != _)
      }
    end readNoCommutativity

    def readInvariants: Property =
      given Repository[CurriedState[MockState]] = createFakeInmemoryRepository

      for {
        generatedTitle <- TodoGenerator.generateRawTitle.forAll
        generatedDescription <- TodoGenerator.generateRawDescription.forAll
        timeLong <- TodoGenerator.generateTime.forAll
        generatedId <- UUIDGenerator.generateUUID.forAll
      } yield {
        val result = for {
          id <- TodoService.save(generatedTitle, generatedDescription).value.run(MockState(generatedId, timeLong)).value._2
          todo <- TodoService.read(id.toString()).value.run(MockState(generatedId, timeLong)).value._2
        } yield (id, todo)

        result match
          case Left(value) => HHResult.failure.log("invariants is failure")
          case Right((id, todo)) => 
            todo.id ==== id and
            todo.createdAt ==== timeLong and
            todo.updatedAt ==== timeLong and
            todo.description ==== generatedDescription and
            todo.title ==== generatedTitle
      }
    end readInvariants

    def readIdempotence: Property =
      given Repository[CurriedState[MockState]] = createFakeInmemoryRepository

      for {
        generatedTitle <- TodoGenerator.generateRawTitle.forAll
        generatedDescription <- TodoGenerator.generateRawDescription.forAll
        timeLong <- TodoGenerator.generateTime.forAll
        generatedId <- UUIDGenerator.generateUUID.forAll
      } yield {
        val firstTime = TodoService.read(generatedId).value.run(MockState(generatedId, timeLong)).value._2
        val secondTime = TodoService.read(generatedId).value.run(MockState(generatedId, timeLong)).value._2
        
        firstTime ==== secondTime
      }
    end readIdempotence
    
  end ReadProperties
  
end TodoServiceProperty
