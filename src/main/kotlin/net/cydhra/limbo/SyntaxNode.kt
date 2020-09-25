package net.cydhra.limbo

sealed class SyntaxNode;

sealed class ExpressionNode : SyntaxNode()

data class EqualityExprNode(
        val leftExpr: ExpressionNode,
        val operator: EqualityOperatorToken, val rightExpr: ExpressionNode) : ExpressionNode()

data class ComparisonExprNode(
        val leftExpr: ExpressionNode,
        val operator: ComparisonOperatorToken,
        val rightExpr: ExpressionNode) : ExpressionNode()

data class AdditionExprNode(
        val leftExpr: ExpressionNode,
        val operator: OperatorToken,
        val rightExpr: ExpressionNode) : ExpressionNode()

data class MultiplicationExprNode(
        val leftExpr: ExpressionNode,
        val operator: OperatorToken,
        val rightExpr: ExpressionNode) : ExpressionNode()

data class UnaryExprNode(val operator: OperatorToken,
                         val expr: ExpressionNode) : ExpressionNode()

data class ConstantExpr(val constant: LiteralToken) : ExpressionNode()