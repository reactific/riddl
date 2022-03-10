package com.reactific.riddl.translator.hugo

import java.net.URL

class HugoTranslatorExamplesTest extends HugoTranslateExamplesBase {

  val output: String = "hugo-translator/target/translator/"
  val roots = Map("Reactive BBQ" -> s"ReactiveBBQ/ReactiveBBQ.riddl",
    "DokN" -> s"dokn/dokn.riddl")

  val geekdoc_url = new URL(
    "file://" + System.getProperty("user.dir") +
      "/hugo-translator/src/test/data/hugo-geekdoc.tar.gz")

  "HugoTranslatorExamplesTest" should {
    for {(name, fileName) <- roots} {
      s"parse, validate, and translate $name" in {
        val options = HugoTranslatingOptions(
          themes =
            Seq("hugo-geekdoc" -> Option(geekdoc_url)),
          sourceURL = Some(
            new URL("https://github.com/reactific/riddl"))
        )
        checkExamples(name, fileName, options)
      }
    }
  }
}
