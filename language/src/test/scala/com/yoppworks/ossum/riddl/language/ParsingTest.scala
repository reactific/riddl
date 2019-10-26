package com.yoppworks.ossum.riddl.language

import java.io.File

import AST.Definition
import AST.RiddlNode
import AST._
import fastparse._
import org.scalatest.MustMatchers
import org.scalatest.WordSpec

import scala.reflect._

case class TestParser(input: RiddlParserInput)
    extends AbstractTopLevelParser
    with MustMatchers {

  def parse[T <: RiddlNode, U <: RiddlNode](
    parser: P[_] => P[T],
    extract: T => U
  ): Either[String, U] = {
    expect(parser) match {
      case Right(content) =>
        Right(extract(content))
      case Left(msg) =>
        Left(msg)
    }
  }

  protected def parserFor[T <: Definition: ClassTag]: P[_] => P[T] = {
    val parser: P[_] => P[_] = classTag[T].runtimeClass match {
      case x if x == classOf[AST.TypeDef]        => typeDef(_)
      case x if x == classOf[AST.DomainDef]      => domainDef(_)
      case x if x == classOf[AST.ContextDef]     => contextDef(_)
      case x if x == classOf[AST.InteractionDef] => interactionDef(_)
      case x if x == classOf[AST.FeatureDef]     => featureDef(_)
      case x if x == classOf[AST.EntityDef]      => entityDef(_)
      case x if x == classOf[AST.AdaptorDef]     => adaptorDef(_)
      case _                                     => domainDef(_)
    }
    parser.asInstanceOf[P[_] => P[T]]
  }

  def parseTopLevelDomains: Either[String, Seq[DomainDef]] = {
    expect(root(_))
  }

  def parseTopLevelDomain[TO <: RiddlNode](
    extract: Seq[DomainDef] => TO
  ): Either[String, TO] = {
    expect(root(_)) match {
      case Right(content) =>
        Right(extract(content))
      case Left(msg) =>
        Left(msg)
    }
  }

  def parseDefinition[FROM <: Definition: ClassTag, TO <: RiddlNode](
    extract: FROM => TO
  ): Either[String, TO] = {
    val parser = parserFor[FROM]
    val result = expect[FROM](parser)
    result.map(extract)
  }

  def parseDefinition[FROM <: Definition: ClassTag]: Either[String, FROM] = {
    val parser = parserFor[FROM]
    expect[FROM](parser)
  }

  def parseDomainDefinition[TO <: RiddlNode](
    extract: DomainDef => TO
  ): Either[String, TO] = {
    parse[DomainDef, TO](domainDef(_), extract)
  }

  def parseContextDefinition[TO <: RiddlNode](
    extract: ContextDef => TO
  ): Either[String, TO] = {
    parse[ContextDef, TO](contextDef(_), extract)
  }
}

/** Unit Tests For ParsingTest */
class ParsingTest extends WordSpec with MustMatchers {

  def parse[T <: RiddlNode, U <: RiddlNode](
    input: RiddlParserInput,
    parser: P[_] => P[T],
    extraction: T => U
  ): Either[String, U] = {
    val tp = TestParser(input)
    tp.parse[T, U](parser, extraction)
  }

  def parseTopLevelDomains(
    input: RiddlParserInput
  ): Either[String, Seq[DomainDef]] = {
    val tp = TestParser(input)
    tp.parseTopLevelDomains
  }

  def parseTopLevelDomain[TO <: RiddlNode](
    input: RiddlParserInput,
    extract: Seq[DomainDef] => TO
  ): Either[String, TO] = {
    val tp = TestParser(input)
    tp.parseTopLevelDomain[TO](extract)
  }

  def parseDomainDefinition[TO <: RiddlNode](
    input: RiddlParserInput,
    extract: DomainDef => TO
  ): Either[String, TO] = {
    val tp = TestParser(input)
    tp.parseDomainDefinition[TO](extract)
  }

  def checkDomainDefinitions[TO <: RiddlNode](
    cases: Map[String, TO],
    extract: DomainDef => TO
  ): Unit = {
    cases foreach {
      case (statement, expected) =>
        val input = s"domain foo {\n$statement\n}"
        val tp = TestParser(RiddlParserInput(input))
        tp.parseDomainDefinition(extract) match {
          case Right(content) =>
            content mustBe expected
          case Left(msg) =>
            fail(msg)
        }
    }
  }

  def parseContextDefinition[TO <: RiddlNode](
    input: RiddlParserInput,
    extract: ContextDef => TO
  ): Either[String, TO] = {
    val tp = TestParser(input)
    tp.parseContextDefinition[TO](extract)
  }

  def parseDefinition[FROM <: Definition: ClassTag, TO <: RiddlNode](
    input: RiddlParserInput,
    extract: FROM => TO
  ): Either[String, TO] = {
    val tp = TestParser(input)
    tp.parseDefinition[FROM, TO](extract)
  }

  def parseDefinition[FROM <: Definition: ClassTag](
    input: RiddlParserInput
  ): Either[String, FROM] = {
    val tp = TestParser(input)
    tp.parseDefinition[FROM]
  }

  def checkDefinition[FROM <: Definition: ClassTag, TO <: RiddlNode](
    input: String,
    expected: TO,
    extract: FROM => TO
  ): Unit = {
    val rip = RiddlParserInput(input)
    TestParser(rip).parseDefinition[FROM] match {
      case Left(msg) => fail(msg)
      case Right(content) =>
        content mustBe expected
    }
  }

  def checkDefinitions[FROM <: Definition: ClassTag, TO <: RiddlNode](
    cases: Map[String, TO],
    extract: FROM => TO
  ): Unit = {
    cases.foreach {
      case (statement: String, expected: TO @unchecked) =>
        val rip = RiddlParserInput(statement)
        TestParser(rip).parseDefinition[FROM] match {
          case Left(msg) => fail(msg)
          case Right(content) =>
            content mustBe expected
        }
    }
  }

  def parseInContext[TO <: RiddlNode](
    input: RiddlParserInput,
    extract: ContextDef => TO
  ): Either[String, TO] = {
    val tp = TestParser(
      RiddlParserInput(s"context foo {\n${input.data}\n}")
    )
    tp.parseContextDefinition[TO](extract)
  }

  def checkContextDefinitions[TO <: RiddlNode](
    cases: Map[String, TO],
    extract: ContextDef => TO
  ): Unit = {
    cases.foreach {
      case (statement: String, expected: TO @unchecked) =>
        val input = s"context foo {\n$statement\n}"
        val tp = TestParser(RiddlParserInput(input))
        tp.parseContextDefinition(extract) match {
          case Right(content) =>
            content mustBe expected
          case Left(msg) =>
            fail(msg)
        }
    }
  }

  def checkFile(label: String, fileName: String): RootContainer = {
    val directory = "language/src/test/input/"
    val file = new File(directory + fileName)
    TopLevelParser.parse(file) match {
      case Left(error) =>
        fail(s"$label:$error")
      case Right(model) =>
        model
    }
  }
}