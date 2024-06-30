package dog.shebang
package fake

import dog.shebang.utility.typeconstructor.CurriedState
import cats.effect.std.UUIDGen
import java.util.UUID
import cats.data.State
import dog.shebang.utility.typeclass.clock.Clock
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import dog.shebang.repository.reader.Reader
import cats.data.EitherT
import dog.shebang.repository.reader.ReadError
import dog.shebang.domain.todo.Todo
import java.{util => ju}
import dog.shebang.repository.creator.Creator
import dog.shebang.repository.creator.CreateError
import dog.shebang.repository.Repository
import cats.effect.IO
import scala.collection.mutable

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

given FakeInmemoryRepository: Repository[CurriedState[MockState]] with {
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
}
