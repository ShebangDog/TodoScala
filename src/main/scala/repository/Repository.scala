package dog.shebang
package repository

import repository.creator.Creator
import repository.reader.Reader
import repository.updator.Updator
import repository.deletor.Deletor

import cats.Monad

trait Repository[F[_] : Monad] extends Creator[F] with Reader[F] with Updator[F] with Deletor[F]
