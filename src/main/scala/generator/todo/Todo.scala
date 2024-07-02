package dog.shebang
package generator.todo

import hedgehog.core.GenT
import hedgehog.{Gen, Range}
import generator.todo.TodoGenerator.generateRawDescription

object TodoGenerator:
  def generateRawTitle: GenT[String] = 
    Gen.string(Gen.alpha, Range.linear(1, 100))
  end generateRawTitle

  def generateRawDescription: GenT[String] =
    Gen.string(Gen.alpha, Range.linear(1, 100))
  end generateRawDescription

  def generateTime: GenT[Long] =
    Gen.long(Range.linear(Long.MinValue / 1000000, Long.MaxValue / 1000000))
  end generateTime
end TodoGenerator