package dog.shebang
package usecase.todoservice

import usecase.todoservice.parse.todo.ParseError
import repository.RepositoryError

sealed trait TodoServiceError
case class TodoRepositoryError(repositoryError: RepositoryError) extends TodoServiceError
