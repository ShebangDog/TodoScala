package dog.shebang
package usecase.todoservice.generate.todo

sealed trait GenerateError

case object ParseTitleError extends GenerateError

case object ParseDescriptionError extends GenerateError

case object ClockError extends GenerateError
