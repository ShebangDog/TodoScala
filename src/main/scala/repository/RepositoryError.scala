package dog.shebang
package repository

import repository.creator.CreateError
import repository.reader.ReadError

sealed trait RepositoryError

case class CreateException(createError: CreateError) extends RepositoryError
case class ReadException(readError: ReadError) extends RepositoryError
