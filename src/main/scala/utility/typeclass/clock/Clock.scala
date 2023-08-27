package dog.shebang
package utility.typeclass.clock

import cats.Monad

import scala.concurrent.duration.FiniteDuration

trait Clock[F[_] : Monad] {
  def realTime: F[FiniteDuration]
}