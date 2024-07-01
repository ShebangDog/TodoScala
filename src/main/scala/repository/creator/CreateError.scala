package dog.shebang
package repository.creator

import usecase.todoservice.generate.todo.GenerateError

sealed trait CreateError

case class ParseException(parseError: GenerateError) extends CreateError
