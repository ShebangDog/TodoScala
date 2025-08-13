package dog.shebang
package repository.reader

import java.util.UUID
import cats.data.EitherT

import domain.todo.Todo
import repository.reader.ReadError

trait Reader[F[_]]:
  def read(id: UUID): EitherT[F, ReadError, Todo]
  def readAll(): EitherT[F, ReadError, List[Todo]]
end Reader
