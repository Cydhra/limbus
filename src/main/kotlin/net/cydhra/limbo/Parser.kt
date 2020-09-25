package net.cydhra.limbo

/**
 * Predictive LL(1) recursive descent parser for the language. I hope.
 */
class Parser(private val tokenStream: Iterator<Token>) {

    /**
     * Token the parser currently looks at. It has not been parsed yet. Null if no more tokens are within the stream.
     */
    private var currentToken: Token?

    /**
     * The next token in the token stream, if any.
     */
    private var peekToken: Token?

    init {
        // read first two non-whitespace tokens
        do {
            currentToken = if (tokenStream.hasNext()) tokenStream.next() else null
        } while (currentToken is WhitespaceToken)

        do {
            peekToken = if (tokenStream.hasNext()) tokenStream.next() else null
        } while (peekToken is WhitespaceToken)
    }

    private fun advance(): Token? {
        val current = currentToken
        currentToken = peekToken

        // skip white spaces
        do {
            peekToken = if (tokenStream.hasNext()) tokenStream.next() else null
        } while (peekToken is WhitespaceToken)

        return current
    }

    fun parse(): SyntaxNode {
        return parseExpression()
    }

    private fun parseExpression(): ExpressionNode {
        return parseEquality()
    }

    private fun parseEquality(): ExpressionNode {
        var expr: ExpressionNode = parseComparison()

        while (currentToken == EqualsEqualsToken || currentToken == BangEqualsToken) {
            val token = advance() as EqualityOperatorToken
            expr = EqualityExprNode(expr, token, parseComparison())
        }

        return expr
    }

    private fun parseComparison(): ExpressionNode {
        return parseAddition()
    }

    private fun parseAddition(): ExpressionNode {
        var expr: ExpressionNode = parseMultiplication()

        while (currentToken == PlusToken || currentToken == MinusToken) {
            val token = advance() as OperatorToken
            expr = AdditionExprNode(expr, token, parseMultiplication())
        }

        return expr
    }

    private fun parseMultiplication(): ExpressionNode {
        return parseUnary()
    }

    private fun parseUnary(): ExpressionNode {
        return parsePrimary()
    }

    private fun parsePrimary(): ExpressionNode {
        return parseNumber()
    }

    private fun parseNumber(): ExpressionNode {
        if (currentToken is LiteralToken) {
            return ConstantExpr(advance() as LiteralToken)
        }

        throw IllegalStateException("not implemented yet")
    }
}