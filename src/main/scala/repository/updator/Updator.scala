package dog.shebang
package repository.updator

import dog.shebang.domain.todo.TodoRefinement
import java.util.UUID
import cats.data.EitherT

trait Updator[F[_]]:
  def update(id: UUID, title: TodoRefinement.Title, description: TodoRefinement.Description): EitherT[F, UpdateError, UUID]
end Updator
