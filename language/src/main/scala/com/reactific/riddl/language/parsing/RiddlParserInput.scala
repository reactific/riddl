/*
 * Copyright 2019 Reactific Software LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reactific.riddl.language.parsing

import com.reactific.riddl.language.Location
import fastparse.ParserInput
import fastparse.internal.Util

import java.io.File
import java.nio.file.Path
import scala.collection.Searching
import scala.io.Source
import scala.language.implicitConversions

/** Same as fastparse.IndexedParserInput but with Location support */
abstract class RiddlParserInput extends ParserInput {
  def origin: Path
  def originName: String
  def data: String
  def root: Path

  override def apply(index: Int): Char = data.charAt(index)
  override def dropBuffer(index: Int): Unit = {}
  override def slice(from: Int, until: Int): String = data.slice(from, until)
  override def length: Int = data.length
  override def innerLength: Int = length
  override def isReachable(index: Int): Boolean = index < length

  def checkTraceable(): Unit = ()

  private[this] lazy val lineNumberLookup: Array[Int] = Util.lineNumberLookup(data)

  /* Uses Linear Search but linNumbersLookup.search (see below) is faster?
  private def lineOf(index: Int): Int = {
    lineNumberLookup.indexWhere(_ > index) match {
      case -1 => 0
      case n  => math.max(0, n - 1)
    }
  }
   */

  private def lineOf(index: Int): Int = {
    val result = lineNumberLookup.search(index)
    result match {
      case Searching.Found(foundIndex) => foundIndex
      case Searching.InsertionPoint(insertionPoint) =>
        if (insertionPoint > 0) insertionPoint - 1 else insertionPoint
      case _ => 0
    }
  }

  def rangeOf(index: Int): (Int, Int) = {
    val line = lineOf(index)
    val start = lineNumberLookup(line)
    val end = lineNumberLookup(line + 1)
    start -> end
  }

  def rangeOf(loc: Location): (Int, Int) = {
    require(loc.line > 0)
    val start = lineNumberLookup(loc.line - 1)
    val end =
      if (lineNumberLookup.length == 1) { data.length }
      else if (loc.line >= lineNumberLookup.length) {
        // actually out of bounds but go to last line
        lineNumberLookup(lineNumberLookup.length - 1)
      } else { lineNumberLookup(loc.line) }
    start -> end
  }

  def location(index: Int, origin: Path = root): Location = {
    val line = lineOf(index)
    val col = index - lineNumberLookup(line)
    Location(line + 1, col + 1, origin)
  }

  def prettyIndex(index: Int): String = { location(index).toString }

  val nl: String = System.getProperty("line.separator")

  def annotateErrorLine(index: Location): String = {
    val (start, end) = rangeOf(index)
    val col = index.col - 1
    slice(start, end).stripTrailing() + nl + " ".repeat(col) + "^" + nl
  }
}

case class StringParserInput(
  data: String,
  originName: String = Location.defaultSourceName)
    extends RiddlParserInput {
  val origin: Path = Path.of(System.getProperty("user.dir"))
  val root: Path = Path.of("<string>")
}

case class FileParserInput(origin: Path) extends RiddlParserInput {

  val data: String = {
    val source: Source = Source.fromFile(origin.toFile)
    try {source.getLines().mkString("\n")}
    finally {source.close()}
  }
  val root: Path = origin.getParent
  def originName: String = origin.getFileName.toString
}

case class SourceParserInput(source: Source, originName: String) extends RiddlParserInput {

  val data: String =
    try {source.mkString}
    finally {source.close()}
  val root: Path = origin.getParent
  def origin: Path = Path.of(System.getProperty("user.dir"))
}

object RiddlParserInput {

  implicit def apply(
    data: String
  ): RiddlParserInput = { StringParserInput(data) }

  implicit def apply(source: Source): RiddlParserInput = {
    SourceParserInput(source, source.descr)
  }

  implicit def apply(file: File): RiddlParserInput = {
    FileParserInput(file.toPath)
  }

  implicit def apply(path: Path): RiddlParserInput = {FileParserInput(path)}
}
