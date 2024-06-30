package dog.shebang

import domain.todo.Todo
import repository.creator.{CreateError, Creator}
import repository.{Repository, RepositoryError}
import usecase.todoservice.TodoServiceError
import usecase.todoservice.impl.TodoService
import usecase.todoservice.impl.TodoService.given
import utility.typeclass.clock.Clock

import cats.Monad
import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.effect.{ExitCode, IO, IOApp, Clock as CatsClock}

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import dog.shebang.repository.reader.Reader
import dog.shebang.repository.reader.ReadError

object Main extends IOApp {
  given UUIDGen[IO] with {
    override def randomUUID: IO[UUID] = IO(UUID.randomUUID())
  }

  given Clock[IO] with {
    override def realTime: IO[FiniteDuration] = CatsClock[IO].realTime
  }

  given Repository[IO] with {
    private val inMemory = mutable.Stack[Todo]()

    override def create(todo: Todo): EitherT[IO, CreateError, UUID] = EitherT.right(
      IO {
        inMemory.push(todo).toList
      }
        .flatMap(IO.println)
        .map(_ => todo.id)
    )

    override def read(id: UUID): EitherT[IO, ReadError, Todo] = 
      val list = inMemory.toList
      val maybeTodo = list.find(_.id == id)

      EitherT.fromOption(maybeTodo, ReadError.NotFoundTodoError)
    end read
  }

  private def program[F[_] : Monad](using UUIDGen[F], Clock[F], Repository[F]): EitherT[F, TodoServiceError, String] = for {
    _ <- TodoService.save[F]("rawTitle", "rawDescription")
    id <- TodoService.save[F]("title", "description")
    _ <- TodoService.save[F]("aaa", "aaaa")
    todo <- TodoService.read(id)
    result <- TodoService.save[F]("rawTitle", "rawDescription")
  } yield s"uuid: $id; todo: $todo"

  private def eitherToExitCode[A, B](either: Either[A, B]): ExitCode = either match {
    case Left(_) => ExitCode.Error
    case Right(_) => ExitCode.Success
  }

  override def run(args: List[String]): IO[ExitCode] =
    program[IO].value
      .map(either => (either.merge.toString, eitherToExitCode(either)))
      .flatMap { case (message, exitCode) => IO.println(message).map(_ => exitCode) }
}
