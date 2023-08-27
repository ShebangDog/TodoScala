package dog.shebang
package repository

import repository.creator.Creator

import cats.Monad

trait Repository[F[_] : Monad] extends Creator[F]
