package dog.shebang
package repository.creator

import usecase.todoservice.parse.todo.ParseError

sealed trait CreateError

case class ParseException(parseError: ParseError) extends CreateError
