package dog.shebang
package usecase.todoservice

import repository.RepositoryError

sealed trait TodoServiceError
case class TodoRepositoryError(repositoryError: RepositoryError) extends TodoServiceError
case class UUIDParseError(error: Throwable) extends TodoServiceError
