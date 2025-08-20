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
import cats.data.{EitherT, NonEmptyList}
import doobie.*
import doobie.implicits.*
import doobie.postgres._
import doobie.postgres.implicits._
import cats.*
import cats.effect.*
import cats.implicits.{catsSyntaxTuple2Semigroupal, *}
import doobie.util.log.LogEvent
import org.scalatest.funspec.AnyFunSpec
import doobie.scalatest.IOChecker
import com.zaxxer.hikari.HikariConfig

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

  private val program1 = 42.pure[ConnectionIO]

  private val program2 = sql"select 42".query[Int].unique

  private val program3: ConnectionIO[(Int, Double)] = {
    val a = sql"select 42".query[Int].unique
    val b = sql"select random()".query[Double].unique

    (a, b).tupled
  }

  private val program4 = sql"select name from country"
    .query[String]
    .to[List]

  private val program5 = sql"select name from country"
    .query[String]
    .stream
    .take(5)
    .compile.toList

  private val program6 = sql"select code, name, population, gnp from country"
    .query[(String, String, Int, Option[Double])]
    .to[List]

  private val program7 = sql"select code, name, population, gnp from country"
    .query[(String, String, Int, Option[Double])]
    .stream
    .take(5)
//    .compile.toList

  private case class Country(code: String, name: String, population: Int, gnp: Option[Double])

  private val program8 = sql"select code, name, population, gnp from country"
    .query[Country]
    .stream
    .take(5)
    .compile.toList

  val trivial = 
    sql"""
      select 42, 'foo'::varchar
    """.query[(Int, String)]

  val update: Update0 = 
    sql"""
      update country set name='new' where name='old'
    """.update

  private def biggerThan(n: Int) =
    sql"select code, name, population, gnp from country where population > $n"
      .query[Country]
      .to[List]

  private def populationIn(range: Range, codes: NonEmptyList[String]): ConnectionIO[List[Country]] = {
    (fr"""
      select code, name, population, gnp
      from country
      where population between ${range.start} and ${range.end}
        and
    """ ++ Fragments.in(fr"code", codes))
      .query[Country].to[List]
  }

  type PersonInfo = (Option[Short], String, Long)
  private def updateBatch(persons: NonEmptyList[Person]): ConnectionIO[Int] = {
    val updateSql = "update person set age = ?, name = ? where id = ?"

    Update[PersonInfo](updateSql).updateMany(
      persons.map(p => (p.age, p.name, p.id))
    )
  }

  private def insert(name: String, age: Option[Short], pets: List[String]): ConnectionIO[Person] =
    sql"""
      insert into person (name, age, pets)
      values ($name, $age, $pets)
    """.update.withUniqueGeneratedKeys("id", "name", "age", "pets")

  private val drop = sql"drop table if exists person".update.run

  private val create =
    sql"""
          create table if not exists person (
            id serial,
            name varchar not null unique,
            age smallint,
            pets varchar[] not null
          )
      """.update.run

  private def updateAge(id: Long, age: Short): Update0 =
    sql"""
      update person set age = $age where id = $id
    """.update

  case class Person(id: Long, name: String, age: Option[Short], pets: List[String])
  private val program10 = sql"""
      select id, name, age, pets from person
  """
    .query[Person].to[List]

  private val printlnSqlLogHandler = new LogHandler[IO] {

    override def run(logEvent: LogEvent): IO[Unit] = {
      IO.println(logEvent.sql)
    }
  }

  private def sample: IO[Unit] = {
    // val nel = NonEmptyList.of(Person("alice", 30), Person("bob", 25), Person("charlie", 35))

    val xa = doobie.util.transactor.Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",  // JDBC driver class name
      url = "jdbc:postgresql://db:5432/world", // Connect URL
      user = "postgres", // Database user name
      password = "test", // Database password
      logHandler = Some(printlnSqlLogHandler)
    )

    for {
      _ <- program1.transact(xa)
      _ <- program2.transact(xa)
      _ <- program3.transact(xa)
      _ <- program4.transact(xa)
      _ <- biggerThan(10000000).transact(xa)
      r <- (drop, create).mapN(_ + _).transact(xa)
      l <- insert("ayumu", Some(26), List("peko")).transact(xa)
      l2 <- insert("hinako", Some(26), List("poko")).transact(xa)
      _ <- updateBatch(NonEmptyList.of(
        Person(id = l.id, name = "ayumu", age = Some(27), pets = List("peko")),
        Person(id = l2.id, name = "hinako2", age = None, pets = List("poko")),
      )).transact(xa)
      _ <- updateAge(l.id, 27).run.transact(xa)
      result <- program10.transact(xa)
//      result <- program5.transact(xa)
      _ <- program8.transact(xa)
      _ <- IO.println(s"Result: $result $l")
    } yield ()
  }

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- sample
//    _ <- program
  } yield ExitCode.Success
}

class Test extends AnyFunSpec with IOChecker {
  val transactor = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",  // JDBC driver class name
      url = "jdbc:postgresql://db:5432/world", // Connect URL
      user = "postgres", // Database user name
      password = "test", // Database password
      logHandler = None
  )

  it("should aaa") {
    check(Main.trivial)
  }

  it("check update") {
    check(Main.update)
  }
}
