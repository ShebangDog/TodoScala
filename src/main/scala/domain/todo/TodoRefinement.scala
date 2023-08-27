package dog.shebang
package domain.todo

import io.github.iltotore.iron.IronType
import io.github.iltotore.iron.constraint.all.MinLength
import io.github.iltotore.iron.constraint.string.Match
import io.github.iltotore.iron.autoRefine


object TodoRefinement {
  type NonEmptyString = MinLength[1]
  type Title = IronType[String, NonEmptyString]
  type Description = IronType[String, NonEmptyString]
  type FormattedTime = IronType[String, Match["[0-9]{4}-((1[0-2])|(0[1-9]))-(([0][1-9])|([1-2][0-9])|([3][0-1]))"]]
}
