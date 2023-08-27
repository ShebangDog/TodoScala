package dog.shebang
package usecase.todoservice.parse.todo

sealed trait ParseError

case object ParseTitleError extends ParseError

case object ParseDescriptionError extends ParseError

case object ClockError extends ParseError
