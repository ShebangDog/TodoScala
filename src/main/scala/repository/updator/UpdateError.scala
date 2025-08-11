package dog.shebang
package repository.updator

enum UpdateError:
  case NotFoundTodoError
  case ParseException(error: String)
end UpdateError