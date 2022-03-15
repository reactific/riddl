package com.reactific.riddl.translator.hugo

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URL
import java.nio.file.Path

class HugoTranslatorTest extends AnyWordSpec with Matchers {

  "HugoTranslator" must {
    "translate geekdoc extras properly" in {
      val options = HugoTranslatingOptions(
        inputFile = Some(Path.of("examples/src/riddl/ReactiveBBQ/ReactiveBBQ.riddl")),
        sourceURL = Some(new URL("https://github.com/reactific/riddl"))
      )
      val state = HugoTranslatorState(options)
      val parents = Seq("domain", "context")
      val file = options.inputFile.get
      val result = HugoTranslator.makeGeekDocExtras(state, parents, file)
      result must contain("geekdocEditPath" -> "/edit/main")
      result must contain("geekdocFilePath" ->
        "examples/src/riddl/ReactiveBBQ/domain/context/entity.riddl")
    }
  }
}
