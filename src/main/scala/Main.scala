package dog.shebang

import domain.todo.Todo
import repository.creator.{CreateError, Creator}
import repository.{Repository, RepositoryError}
import usecase.todoservice.TodoServiceError
import usecase.todoservice.impl.TodoService
import utility.typeclass.clock.Clock
import io.github.iltotore.iron.autoRefine

import cats.Monad
import cats.effect.std.UUIDGen
import cats.effect.{ExitCode, IO, IOApp, Clock as CatsClock}

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import cats.data.EitherT

object Main extends IOApp {
  import dog.shebang.fake.InmemoryRepository

  given UUIDGen[IO] with {
    override def randomUUID: IO[UUID] = IO(UUID.randomUUID())
  }

  given Clock[IO] with {
    override def realTime: IO[FiniteDuration] = CatsClock[IO].realTime
  }

  private def readUserInput: IO[String] = for {
    _ <- IO.print("コマンドを入力してください (save/read/readAll/update/delete/exit): ")
    input <- IO.readLine
  } yield input

  private def handleUserCommand[F[_] : Monad](command: String)(using UUIDGen[F], Clock[F], Repository[F]): EitherT[F, TodoServiceError, String] = 
    command.split(" ").toList match {
      case "save" :: title :: description :: Nil =>
        TodoService.save[F](title, description).map(id => s"保存されました: $id")
      case "read" :: id :: Nil =>
        TodoService.read(id).map(_.toString)
      case "readAll" :: Nil =>
        TodoService.readAll().map(_.toString)
      case "update" :: id :: title :: description :: Nil =>
        TodoService.update(id, title, description).map(_.toString()).map(id => s"更新されました: $id")
      case "delete" :: id :: Nil =>
        TodoService.delete(id).map(_.toString()).map(id => s"削除されました: $id")
      case _ =>
        EitherT.pure("無効なコマンドです")
    }

  private def program: IO[ExitCode] = for {
    input <- readUserInput
    _ <- if (input == "exit") IO.pure(ExitCode.Success)
         else for {
           result <- handleUserCommand[IO](input).value
           _ <- IO.println(result.merge.toString)
           _ <- program
         } yield ExitCode.Error
  } yield ExitCode.Error


  private def eitherToExitCode[A, B](either: Either[A, B]): ExitCode = either match {
    case Left(_) => ExitCode.Error
    case Right(_) => ExitCode.Success
  }

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- program
  } yield ExitCode.Success
}
