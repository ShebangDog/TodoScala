package dog.shebang
package domain.todo

case class Todo(id: String, title: String, description: String, createdAt: String, updatedAt: String)

object Todo {
  def isTitleFilled(todo: Todo): Boolean = todo.title.nonEmpty

  def isDescriptionFilled(todo: Todo): Boolean = todo.description.nonEmpty
}
