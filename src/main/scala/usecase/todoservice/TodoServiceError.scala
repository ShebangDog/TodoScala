package dog.shebang
package usecase.todoservice

import repository.RepositoryError

sealed trait TodoServiceError
case class TodoRepositoryError(repositoryError: RepositoryError) extends TodoServiceError
