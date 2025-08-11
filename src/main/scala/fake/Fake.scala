package dog.shebang
package fake

import dog.shebang.utility.typeconstructor.CurriedState
import cats.effect.std.UUIDGen
import java.util.UUID
import cats.data.State
import dog.shebang.utility.typeclass.clock.Clock
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import dog.shebang.repository.reader.{Reader, ReadError}
import cats.data.EitherT
import dog.shebang.domain.todo.Todo
import java.{util => ju}
import dog.shebang.repository.creator.{Creator, CreateError}
import dog.shebang.repository.updator.{Updator, UpdateError}
import dog.shebang.repository.deletor.{Deletor, DeleteError}
import dog.shebang.repository.Repository
import cats.effect.IO
import scala.collection.mutable
import dog.shebang.domain.todo.TodoRefinement.Description
import dog.shebang.domain.todo.TodoRefinement.Title
import dog.shebang.domain.todo.TodoRefinement

case class MockState(rawUuid: String, nowTime: Long)

given FakeUUIDGen: UUIDGen[CurriedState[MockState]] with {
  override def randomUUID: CurriedState[MockState][UUID] = State[MockState, UUID] { mockState =>
    val MockState(uuid, _) = mockState

    (mockState, UUID.fromString(uuid))
  }
}

given FakeClock: Clock[CurriedState[MockState]] with {
  override def realTime: CurriedState[MockState][FiniteDuration] = State[MockState, FiniteDuration] { mockState =>
    val MockState(_, nowTime) = mockState

    (mockState, FiniteDuration(nowTime, TimeUnit.MILLISECONDS))
  }
}

def createFakeInmemoryRepository = new Repository[CurriedState[MockState]] {
  private val inMemory = mutable.Stack[Todo]()

  override def create(todo: Todo): EitherT[CurriedState[MockState], CreateError, UUID] = 
    inMemory.push(todo)

    EitherT.right(
      State(initial => (initial, UUID.fromString(initial.rawUuid)))
    )
  end create

  override def read(id: UUID): EitherT[CurriedState[MockState], ReadError, Todo] = 
    val list = inMemory.toList
    val maybeTodo = list.find(_.id == id)

    EitherT.fromOption(maybeTodo, ReadError.NotFoundTodoError)
  end read

  override def readAll(): EitherT[CurriedState[MockState], ReadError, List[Todo]] = 
    val list = inMemory.toList

    EitherT.right(State(initial => (initial, list)))
  end readAll

  override def update(id: UUID, title: TodoRefinement.Title, description: TodoRefinement.Description): EitherT[CurriedState[MockState], UpdateError, UUID] =
    val inMemoryList = inMemory.toList
    val todoIndex = inMemoryList.indexWhere(_.id == id)
    val maybeTodo = inMemoryList.lift(todoIndex)

    maybeTodo match {
      case None => EitherT.leftT(UpdateError.NotFoundTodoError)
      case Some(todo) =>
        inMemory.update(todoIndex, todo.copy(title = title, description = description))

        EitherT.right(State(initial => (initial, todo.id)))
    }
  end update

  override def delete(id: UUID): EitherT[CurriedState[MockState], DeleteError, UUID] =
    val list = inMemory.toList
    val maybeTodo = list.find(_.id == id)

    maybeTodo match {
      case None => EitherT.leftT(DeleteError.NotFound)
      case Some(todo) =>
        // Stackから該当のTodoを削除
        inMemory.filterInPlace(_.id != id)
        EitherT.right(State(initial => (initial, todo.id)))
    }
  end delete
}

given InmemoryRepository: Repository[IO] with {
  private val inMemory = mutable.Stack[Todo]()

  override def create(todo: Todo): EitherT[IO, CreateError, UUID] = 
    EitherT.right(
      for {
        io <- IO { inMemory.push(todo) }
        _ <- IO.println(todo)
      } yield todo.id
    )
  end create

  override def read(id: UUID): EitherT[IO, ReadError, Todo] = 
    val list = inMemory.toList
    val maybeTodo = list.find(_.id == id)

    EitherT.fromOption(maybeTodo, ReadError.NotFoundTodoError)
  end read

  override def readAll(): EitherT[IO, ReadError, List[Todo]] = 
    val list = inMemory.toList

    EitherT.rightT(list)
  end readAll

  override def update(id: UUID, title: Title, description: Description): EitherT[IO, UpdateError, UUID] = 
    val inMemoryList = inMemory.toList
    val todoIndex = inMemoryList.indexWhere(_.id == id)
    val maybeTodo = inMemoryList.lift(todoIndex)

    maybeTodo match {
      case None => EitherT.leftT(UpdateError.NotFoundTodoError)
      case Some(todo) =>
        inMemory.update(todoIndex, todo.copy(title = title, description = description))

        EitherT.rightT(todo.id)
    }
  end update

  override def delete(id: UUID): EitherT[IO, DeleteError, UUID] = 
    val list = inMemory.toList
    val maybeTodo = list.find(_.id == id)

    maybeTodo match {
      case None => EitherT.leftT(DeleteError.NotFound)
      case Some(todo) =>
        inMemory.filterInPlace(_.id != id)
        EitherT.rightT(todo.id)
    }
  end delete
}
