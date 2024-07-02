package dog.shebang
package usecase.todoservice.generate.todo

import domain.todo.{Todo, TodoUtil}
import utility.typeclass.clock.Clock

import cats.Monad
import cats.syntax.functor.*
import cats.data.EitherT
import cats.effect.std.UUIDGen

def generateTodo[F[_] : Monad](using generator: UUIDGen[F], clock: Clock[F])
  (rawTitle: String, rawDescription: String): EitherT[F, GenerateError, Todo] = for {
  title <- EitherT.fromEither[F](TodoUtil.refineTitle(rawTitle)).leftMap(_ => ParseTitleError)
  description <- EitherT.fromEither[F](TodoUtil.refineDescription(rawDescription)).leftMap(_ => ParseDescriptionError)
  id <- EitherT.right(TodoUtil.generateId)
  createdAt <- EitherT.right(clock.realTime.map(_.toMillis)).leftMap(_ => ClockError)
  updatedAt = createdAt
} yield Todo(id, title, description, createdAt, updatedAt)

import hedgehog.runner.{Properties, Test, property}
import hedgehog.{Property, Syntax}
import hedgehog.core.Result
import java.util.UUID
import generator.todo.TodoGenerator
import generator.uuid.UUIDGenerator

object GenerateTodoProperty extends Properties:
  import dog.shebang.fake.{ FakeClock, FakeUUIDGen, MockState }

  override def tests: List[Test] = 
    List(
      property("symmetry", symmetry)
    )
  end tests

  def symmetry: Property = for {
    rawTitle <- TodoGenerator.generateRawTitle.forAll
    rawDescription <- TodoGenerator.generateRawDescription.forAll
    rawUUID <- UUIDGenerator.generateUUID.forAll
    time <- TodoGenerator.generateTime.forAll
  } yield {
    
    val (_, todoResult) = generateTodo(rawTitle, rawDescription).value.run(MockState(rawUUID, time)).value

    todoResult match
      case Left(_) => Result.failure.log("failure")
      case Right(todo) => 
        todo.title ==== rawTitle and
        todo.id ====  UUID.fromString(rawUUID) and
        todo.description ==== rawDescription and
        todo.createdAt ==== time and
        todo.updatedAt ==== time
  }
  end symmetry

end GenerateTodoProperty
