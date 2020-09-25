package net.cydhra.limbo

sealed class Token {
    // for non-data class subclasses
    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

sealed class Parenthesis : Token()

sealed class OperatorToken : Token()

sealed class LiteralToken : Token()

object PlusToken : OperatorToken()
object MinusToken : OperatorToken()
object TimesToken : OperatorToken()
object DivideToken : OperatorToken()

sealed class EqualityOperatorToken : OperatorToken()
object EqualsEqualsToken : EqualityOperatorToken()
object BangEqualsToken : EqualityOperatorToken()

sealed class ComparisonOperatorToken : OperatorToken()
object GreaterToken : ComparisonOperatorToken()
object LesserToken : ComparisonOperatorToken()
object GreaterEqualsToken : ComparisonOperatorToken()
object LesserEqualsToken : ComparisonOperatorToken()

object PlusPlusToken : OperatorToken()
object MinusMinusToken : OperatorToken()

data class IntegerLiteralToken(val literal: String) : LiteralToken()
data class FloatingPointLiteralToken(val literal: String) : LiteralToken()

/**
 * An unexpected token in the input stream
 */
data class ErroneousToken(val unexpectedToken: String) : Token()

/**
 * Special token for white space, so it can later be used to reconstruct the input
 */
data class WhitespaceToken(val whitespace: String) : Token()