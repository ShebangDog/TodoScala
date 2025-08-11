package dog.shebang
package repository.deletor

import cats.data.EitherT
import cats.effect.IO
import dog.shebang.domain.todo.Todo
import java.util.UUID

trait Deletor[F[_]] {
  def delete(id: UUID): EitherT[F, DeleteError, UUID]
}
