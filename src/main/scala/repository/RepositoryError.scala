package dog.shebang
package repository

import repository.creator.CreateError
import repository.reader.ReadError
import repository.updator.UpdateError

sealed trait RepositoryError

case class CreateException(createError: CreateError) extends RepositoryError
case class ReadException(readError: ReadError) extends RepositoryError
case class UpdateException(updateError: UpdateError) extends RepositoryError
