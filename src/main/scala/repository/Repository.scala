package dog.shebang
package repository

import repository.creator.Creator
import repository.reader.Reader

import cats.Monad

trait Repository[F[_] : Monad] extends Creator[F] with Reader[F]
