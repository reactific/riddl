package com.yoppworks.ossum.riddl.language

import AST._
import Terminals._
import fastparse._
import ScalaWhitespace._

import scala.collection.immutable.ListMap

/** Parser for an entity handler definition */
trait HandlerParser
    extends CommonParser with ConditionParser with ExpressionParser {

  def setStmt[_: P](): P[SetStatement] = {
    P(
      Keywords.set ~/ location ~ pathIdentifier ~ Terminals.Readability.to ~
        expression ~ description
    ).map { t => (SetStatement.apply _).tupled(t) }
  }

  def appendStmt[_: P]: P[AppendStatement] = {
    P(
      Keywords.append ~/ location ~ pathIdentifier ~ Terminals.Readability.to ~
        identifier ~ description
    ).map { t => (AppendStatement.apply _).tupled(t) }
  }

  def messageConstructor[_: P]: P[MessageConstructor] = {
    P(messageRef ~ argList.?).map(tpl => {
      val args = tpl._2 match {
        case None    => ListMap.empty[Identifier, Expression]
        case Some(a) => a
      }
      MessageConstructor(tpl._1, args)
    })
  }

  def publishStmt[_: P]: P[PublishStatement] = {
    P(
      (Keywords.yields | Keywords.publish) ~/ location ~ messageConstructor ~
        Terminals.Readability.to ~ topicRef ~ description
    ).map { t => (PublishStatement.apply _).tupled(t) }
  }

  def sendStmt[_: P]: P[SendStatement] = {
    P(
      Keywords.send ~/ location ~ messageConstructor ~ Readability.to ~
        entityRef ~ description
    ).map { t => (SendStatement.apply _).tupled(t) }
  }

  def removeStmt[_: P](): P[RemoveStatement] = {
    P(
      Keywords.remove ~/ location ~ pathIdentifier ~ Readability.from ~
        pathIdentifier ~ description
    ).map { t => (RemoveStatement.apply _).tupled(t) }
  }

  def executeStmt[_: P]: P[ExecuteStatement] = {
    P(location ~ Keywords.execute ~/ identifier ~ description).map { t =>
      (ExecuteStatement.apply _).tupled(t)
    }
  }

  def whenStmt[_: P]: P[WhenStatement] = {
    P(
      location ~ Keywords.when ~/ condition ~ Keywords.then_.? ~
        Punctuation.curlyOpen ~/ onClauseAction.rep ~ Punctuation.curlyClose ~
        description
    ).map(t => (WhenStatement.apply _).tupled(t))
  }

  def onClauseAction[_: P]: P[OnClauseStatement] = {
    P(
      setStmt | appendStmt | removeStmt | sendStmt | publishStmt | whenStmt |
        executeStmt
    )
  }

  def onClause[_: P]: P[OnClause] = {
    Keywords.on ~/ location ~ messageRef ~ open ~ onClauseAction.rep ~ close ~
      description
  }.map(t => (OnClause.apply _).tupled(t))

  def handler[_: P]: P[Handler] = {
    P(
      Keywords.handler ~/ location ~ identifier ~ is ~
        ((open ~ undefined ~ close).map(_ => Seq.empty[OnClause]) |
          optionalNestedContent(onClause)) ~ description
    ).map(t => (Handler.apply _).tupled(t))
  }
}