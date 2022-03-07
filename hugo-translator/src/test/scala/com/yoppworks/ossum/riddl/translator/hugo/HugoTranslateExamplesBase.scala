package com.yoppworks.ossum.riddl.translator.hugo

import com.yoppworks.ossum.riddl.language.{CommonOptions, SysLogger, ValidatingTest}
import org.scalatest.Assertion

import java.io.File
import java.nio.file.{Files, Path}
import scala.collection.mutable.ArrayBuffer

abstract class HugoTranslateExamplesBase extends ValidatingTest {

  val directory: String = "examples/src/riddl/"
  val output: String

  def makeSrcDir(path: String): Path = {
    Path.of(output).resolve(path)
  }
  val commonOptions: CommonOptions =  CommonOptions(
    showTimes = true,
    showWarnings = false,
    showMissingWarnings = false,
    showStyleWarnings = false
  )

  def genHugo(
    projectName: String,
    source: String,
    hugoOptions: HugoTranslatingOptions = HugoTranslatingOptions()
  ): Seq[Path] = {
    val outDir = Path.of(output).resolve(source)
    val outDirFile = outDir.toFile
    if (!outDirFile.isDirectory) outDirFile.mkdirs()
    val sourcePath = Path.of(directory).resolve(source)
    val htc = hugoOptions.copy(
      inputFile = Some(sourcePath),
      outputDir = Some(outDir),
      eraseOutput = true,
      projectName = Some(projectName)
    )
    val ht = HugoTranslator
    ht.parseValidateTranslate(SysLogger(), commonOptions, htc)
  }

  def runHugo(source: String): Assertion = {
    import scala.sys.process._
    val lineBuffer: ArrayBuffer[String] = ArrayBuffer[String]()
    var hadErrorOutput: Boolean = false
    var hadWarningOutput: Boolean = false

    def fout(line: String): Unit = {
      lineBuffer.append(line)
      if (!hadWarningOutput && line.contains("WARN")) hadWarningOutput = true
    }

    def ferr(line: String): Unit = { lineBuffer.append(line); hadErrorOutput = true }

    val logger = ProcessLogger(fout, ferr)
    val srcDir = makeSrcDir(source)
    Files.isDirectory(srcDir)
    val cwdFile = srcDir.toFile
    val proc = Process("hugo", cwd = Option(cwdFile))
    proc.!(logger) match {
      case 0 =>
        if (hadErrorOutput) { fail("hugo wrote to stderr:\n  " + lineBuffer.mkString("\n  ")) }
        else if (hadWarningOutput) {
          fail("hugo issued warnings:\n  " + lineBuffer.mkString("\n  "))
        } else { succeed }
      case rc: Int => fail(s"hugo run failed with rc=$rc:\n  " + lineBuffer.mkString("\n  "))
    }
  }

  def checkExamples(
    name: String,
    path: String,
    options: HugoTranslatingOptions = HugoTranslatingOptions()
  ): Assertion = {
    // translation must have happened
    genHugo(name, path, options) mustNot be(empty)
    runHugo(path)
  }
}
