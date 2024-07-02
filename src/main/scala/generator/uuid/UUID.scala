package dog.shebang
package generator.uuid

import hedgehog.{Gen, Range}
import hedgehog.core.GenT

object UUIDGenerator:
  def generateUUID: GenT[String] =
    def generateUUIDSection(counts: Int): GenT[String] = 
      Gen.string(Gen.hexit, Range.singleton(counts))
    end generateUUIDSection

    for {
      first <- generateUUIDSection(8)
      second <- generateUUIDSection(4)
      third <- generateUUIDSection(4)
      four <- generateUUIDSection(4)
      five <- generateUUIDSection(12)
    } yield List(first, second, third, four, five).mkString("-")
  end generateUUID
end UUIDGenerator