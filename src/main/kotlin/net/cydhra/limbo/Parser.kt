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
        // TODO: this should probably not fail on an empty stream (because it is hard to see that the stream is empty
        //  from the outside, if it contains whitespace. However, this is a temporary function anyway, which later
        //  should parse the entire stream at once
        if (currentToken != null)
            return parseExpression()
        else
            error("token stream is empty")
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
        var expr: ExpressionNode = parseAddition()

        while (currentToken == LesserToken
                || currentToken == GreaterToken
                || currentToken == LesserEqualsToken
                || currentToken == GreaterEqualsToken) {
            val token = advance() as ComparisonOperatorToken
            expr = ComparisonExprNode(expr, token, parseAddition())
        }

        return expr
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
        var expr: ExpressionNode = parseUnary()

        while (currentToken == TimesToken || currentToken == DivideToken) {
            val token = advance() as OperatorToken
            expr = MultiplicationExprNode(expr, token, parseUnary())
        }

        return expr
    }

    private fun parseUnary(): ExpressionNode {
        if (currentToken == MinusToken || currentToken == BangToken) {
            val token = advance() as OperatorToken
            return UnaryExprNode(token, parsePrimary())
        }

        return parsePrimary()
    }

    private fun parsePrimary(): ExpressionNode {
        return when (currentToken) {
            is LiteralToken -> parseNumber()
            LeftParenthesisToken -> {
                advance()
                val expr = parseExpression()
                if (advance() == RightParenthesisToken) {
                    expr
                } else {
                    throw IllegalArgumentException("missing closing parenthesis")
                }
            }

            else -> throw IllegalStateException("not implemented yet")
        }
    }

    private fun parseNumber(): ExpressionNode {
        return ConstantExpr(advance() as LiteralToken)
    }
}