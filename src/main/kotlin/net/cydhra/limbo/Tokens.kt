package net.cydhra.limbo

sealed class Token {
    // for non-data class subclasses
    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

sealed class Parenthesis : Token()

sealed class Operator : Token()

sealed class Literal : Token()

object PlusToken : Operator()
object MinusToken : Operator()
object TimesToken : Operator()
object DivideToken : Operator()

object PlusPlusToken : Operator()
object MinusMinusToken : Operator()

data class IntegerLiteral(val literal: String) : Literal()
data class FloatingPointLiteral(val literal: String) : Literal()

/**
 * An unexpected token in the input stream
 */
data class ErroneousToken(val unexpectedToken: String) : Token()

/**
 * Special token for white space, so it can later be used to reconstruct the input
 */
data class WhitespaceToken(val whitespace: String) : Token()