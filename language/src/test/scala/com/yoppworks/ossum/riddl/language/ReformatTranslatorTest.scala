package com.yoppworks.ossum.riddl.language

import com.yoppworks.ossum.riddl.language.parsing.RiddlParserInput
import org.scalatest.Assertion

import java.io.File
import java.nio.file.Path

/** Test The ReformatTranslator's ability to generate consistent output */
class ReformatTranslatorTest extends RiddlFilesTestBase {

  def checkAFile(rootDir: Path, file: File): Assertion = {
    val input = RiddlParserInput(file)
    parseTopLevelDomains(input) match {
      case Left(errors) =>
        val msg = errors.map(_.format).mkString
        fail(msg)
      case Right(roots) =>
        val options = ReformattingOptions(inputFile = Some(file.toPath))
        val log = SysLogger()
        val common = CommonOptions()
        val output = ReformatTranslator.translateToString(roots, log, common, options)
        parseTopLevelDomains(output) match {
          case Left(errors) =>
            val message = errors.map(_.format).mkString("\n")
            fail(
              s"In '${file.getPath}': on first generation:\n" + message + "\nIn Source:\n" +
                output + "\n"
            )
          case Right(roots2) =>
            val output2 = ReformatTranslator.translateToString(roots2, log, common, options)
            parseTopLevelDomains(output2) match {
              case Left(errors) =>
                val message = errors.map(_.format).mkString("\n")
                fail(
                  s"In '${file.getPath}': on second generation: " + message + "\nIn Source:\n" +
                    output2 + "\n"
                )
              case Right(_) => output mustEqual output2
            }
        }
    }
    succeed
  }

  val items = Seq(
    "examples/src/riddl/ReactiveBBQ/ReactiveBBQ.riddl" -> false,
    "doc/src/hugo/content/language/hierarchy/domain/streaming/riddl/plant.riddl" -> false,
    "language/src/test/input/domains" -> true,
    "language/src/test/input/enumerations" -> true,
    "language/src/test/input/mappings" -> true,
    "language/src/test/input/ranges" -> true,
    "language/src/test/input/empty.riddl" -> false,
    "language/src/test/input/everything.riddl" -> false,
    "language/src/test/input/petstore.riddl" -> false,
    "language/src/test/input/rbbq.riddl" -> false
  )

  "ReformatTranslator" should { checkItems(items) }
}