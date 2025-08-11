package dog.shebang

import domain.todo.Todo
import repository.creator.{CreateError, Creator}
import repository.{Repository, RepositoryError}
import usecase.todoservice.TodoServiceError
import usecase.todoservice.impl.TodoService
import utility.typeclass.clock.Clock
import io.github.iltotore.iron.autoRefine

import cats.Monad
import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.effect.{ExitCode, IO, IOApp, Clock as CatsClock}

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object Main extends IOApp {
  import dog.shebang.fake.InmemoryRepository

  given UUIDGen[IO] with {
    override def randomUUID: IO[UUID] = IO(UUID.randomUUID())
  }

  given Clock[IO] with {
    override def realTime: IO[FiniteDuration] = CatsClock[IO].realTime
  }

  private def program[F[_] : Monad](using UUIDGen[F], Clock[F], Repository[F]): EitherT[F, TodoServiceError, String] = for {
    _ <- TodoService.save[F]("rawTitle", "rawDescription")
    id <- TodoService.save[F]("title", "description")
    _ <- TodoService.save[F]("aaa", "aaaa")
    _ <- TodoService.update[F](id, "updatedTitle", "updatedDescription")
    todo <- TodoService.read[F](id)
    _ <- TodoService.delete[F](id)
    todoList <- TodoService.readAll()
    todoListFlatten = todoList.foldLeft("")((acc, todo) => acc + s"uuid: ${todo.id}; title: ${todo.title}; description: ${todo.description}\n")
    result <- TodoService.save[F]("rawTitle", "rawDescription")
  } yield todoListFlatten

  private def eitherToExitCode[A, B](either: Either[A, B]): ExitCode = either match {
    case Left(_) => ExitCode.Error
    case Right(_) => ExitCode.Success
  }

  override def run(args: List[String]): IO[ExitCode] = 
    val io = program[IO].value
    
    for {
      result <- io
      message = result.merge.toString
      exitCode = eitherToExitCode(result)
      _ <- IO.println(message)
    } yield exitCode
  end run
}
