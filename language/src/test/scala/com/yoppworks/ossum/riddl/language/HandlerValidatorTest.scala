package com.yoppworks.ossum.riddl.language

import com.yoppworks.ossum.riddl.language.AST.Domain
import com.yoppworks.ossum.riddl.language.AST.Entity
import com.yoppworks.ossum.riddl.language.Validation._

class HandlerValidatorTest extends ValidatingTest {

  "Handler Validation" should {
    "produce an error when on clause references a command that does not exist" in {
      val input = """
                    |domain entityTest is {
                    |topic EntityChannel is {}
                    |context EntityContext is {
                    |entity Hamburger is {
                    |  state HamburgerState = { field1: Number, field2: String }
                    |  handler foo is {
                    |    on command EntityCommand { set field1 to 445 }
                    |    on event EntityEvent { set field1 to 678 }
                    |  }
                    |}
                    |}
                    |}
                    |""".stripMargin
      parseAndValidate[Domain](input) {
        case (d: Domain, msgs: ValidationMessages) =>
          assertValidationMessage(
            msgs,
            Validation.Error,
            "'EntityCommand' is not defined but should be a Command"
          )
          assertValidationMessage(
            msgs,
            Validation.Error,
            "'EntityEvent' is not defined but should be a Event"
          )
      }
    }

    "produce an error when on clause references a message of the wrong type" in {
      val input = """
                    |domain entityTest is {
                    |context EntityContext is {
                    |entity Hamburger is {
                    |  state HamburgerState = { field1: Number }
                    |  handler foo is {
                    |    on event Incoming { set field1 to 678 }
                    |  }
                    |}
                    |}
                    |}
                    |""".stripMargin
      parseAndValidate[Domain](input) { case (_, msgs: ValidationMessages) =>
        // TODO:  ideally this would indicate the def does exist but the wrong type
        assertValidationMessage(
          msgs,
          Validation.Error,
          "'Incoming' is not defined but should be a Event"
        )
      }
    }

    "produce an error when on clause references a state field that does not exist" in {
      val input =
        """
          |domain entityTest is {
          |context EntityContext is {
          |entity Hamburger is {
          |  state HamburgerState = { field1: Number }
          |  handler foo is {
          |    on command EntityCommand { set nonExistingField to 123 }
          |  }
          |}
          |}
          |}
          |""".stripMargin
      parseAndValidate[Domain](input) { case (_, msgs: ValidationMessages) =>
        assertValidationMessage(
          msgs,
          Validation.Error,
          "'nonExistingField' is not defined but should be a Field"
        )
      }
    }

    "produce an error when on clause sets state from a message field that does not exist" in {
      pending
    }

    "produce an error when on clause sets state from incompatible type of message field" in {
      pending
    }
  }
}