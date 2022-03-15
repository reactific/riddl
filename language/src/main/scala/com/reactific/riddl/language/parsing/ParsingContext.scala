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

import com.reactific.riddl.language.AST.*
import com.reactific.riddl.language.Location
import fastparse.*
import fastparse.Parsed.{Failure, Success}
import fastparse.internal.Lazy

import java.nio.file.{Files, Path}
import scala.annotation.unused
import scala.collection.mutable

case class ParserError(
  input: RiddlParserInput,
  loc: Location,
  msg: String,
  context: String = "")
    extends Throwable {

  def format: String = {
    val errorLine = input.annotateErrorLine(loc)
    s"Error: ${input.origin}$loc: $msg but got:\n${errorLine}Context: $context"
  }
}

/** Unit Tests For ParsingContext */
trait ParsingContext {

  protected val stack: InputStack = InputStack()

  protected val errors: mutable.ListBuffer[ParserError] =
    mutable.ListBuffer.empty[ParserError]

  def current: RiddlParserInput = { stack.current }

  def location[u: P]: P[Location] = {
    val cur = current
    val relative = stack.stackRoot.relativize(cur.root).resolve(cur.origin)
    P(Index).map(idx => cur.location(idx, relative))
  }

  def doImport(loc: Location, domainName: Identifier, fileName: LiteralString): Domain = {
    val name = fileName.s
    val file = current.root.resolve(name)
    if (!Files.exists(file)) {
      error(fileName.loc,
        s"File '$name` does not exist, can't be imported.")
      Domain(loc, domainName)
    } else {importDomain(file)}
  }

  def importDomain(
    @unused
    file: Path
  ): Domain = {
    // TODO: implement importDomain
    Domain(Location(), Identifier(Location(), "NotImplemented"))
  }

  def doInclude[T <: Definition](str: LiteralString)(
    rule: P[?] => P[Seq[T]]
  ): Include = {
    val name = str.s + ".riddl"
    val path = current.root.resolve(name)
    if (Files.exists(path) && !Files.isHidden(path)) {
      if (Files.isReadable(path)) {
        stack.push(path)
        try {
          this.expect[Seq[T]](rule) match {
            case Left(theErrors) =>
              theErrors.foreach(errors.append)
              Include(str.loc, Seq.empty[T], Some(path))
            case Right(parseResult) =>
              Include(str.loc, parseResult, Some(path))
          }
        } finally {
          stack.pop
        }
      } else {
        error(str.loc,
          s"File '$name' exits but can't be read, so it can't be included.")
        Include(str.loc, Seq.empty[ParentDefOf[Definition]],  Some(path))
      }
    } else {
      error(str.loc,
        s"File '$name' does not exist, so it can't be included.")
      Include(str.loc, Seq.empty[ParentDefOf[Definition]], Some(path))
    }
  }

  def error(loc: Location, msg: String, context: String = ""): Unit = {
    val error = ParserError(current, loc, msg, context)
    errors.append(error)
  }

  private def mkTerminals(list: List[Lazy[String]]): String = {
    list.map(_.force).map {
      case s: String if s.startsWith("char-pred")  => "pattern"
      case s: String if s.startsWith("chars-with") => "pattern"
      case s: String                               => s
    }.distinct.mkString("(", " | ", ")")
  }

  def makeParseFailureError(failure: Failure): Unit = {
    val location = current.location(failure.index)
    val trace = failure.trace()
    val msg = trace.terminals.value.size match {
      case 0 => "Unexpected content"
      case 1 => s"Expected " + mkTerminals(trace.terminals.value)
      case _ => s"Expected one of " + mkTerminals(trace.terminals.value)
    }
    val context = trace.groups.render
    error(location, msg, context)
  }

  def expect[T](parser: P[?] => P[T]): Either[Seq[ParserError], T] = {
    fastparse.parse(current, parser(_)) match {
      case Success(content, _) =>
        if (errors.nonEmpty) { Left(errors.toSeq) }
        else { Right(content) }
      case failure: Failure =>
        makeParseFailureError(failure)
        Left(errors.toSeq)
      case _ =>
        throw new IllegalStateException(
          "Parsed[T] should have matched Success or Failure"
        )
    }
  }
}
