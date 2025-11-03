package evaluator

import lexer.*
import parser.*

/**
 * RuntimeError - Exception thrown when evaluation fails
 * Made public so it can be caught in Main.kt
 */
class RuntimeError(val token: Token, message: String) : RuntimeException(message)

/**
 * Evaluator - Traverses the AST and computes values
 * Uses the Visitor pattern to evaluate different node types
 */
class Evaluator {

    /**
     * Main entry point - evaluates a program or expression
     */
    fun evaluate(node: AstNode): Any? {
        return when (node) {
            is Program -> evaluateProgram(node)
            is Stmt -> evaluateStatement(node)
            is Expr -> evaluateExpression(node)
            else -> throw RuntimeError(
                Token(TokenType.EOF, "", null, 0),
                "Unknown AST node type"
            )
        }
    }

    // ----------------------
    // PROGRAM EVALUATION
    // ----------------------

    private fun evaluateProgram(program: Program): Any? {
        var lastValue: Any? = null
        for (stmt in program.stmtList) {
            lastValue = evaluateStatement(stmt)
        }
        return lastValue
    }

    // ----------------------
    // STATEMENT EVALUATION
    // ----------------------

    private fun evaluateStatement(stmt: Stmt): Any? {
        return when (stmt) {
            is ExprStmt -> evaluateExpression(stmt.expression)
            is PrintStmt -> evaluatePrintStmt(stmt)
            is VarDeclStmt -> evaluateVarDeclStmt(stmt)
            is Block -> evaluateBlock(stmt)
            is IfStmt -> evaluateIfStmt(stmt)
            is DefineStmt -> evaluateDefineStmt(stmt)
            is ExploreStmt -> evaluateExploreStmt(stmt)
            is ThrowBallStmt -> evaluateThrowBallStmt(stmt)
            is RunStmt -> evaluateRunStmt(stmt)
        }
    }

    private fun evaluatePrintStmt(stmt: PrintStmt): Any? {
        val value = evaluateExpression(stmt.expression)
        println(stringify(value))
        return null
    }

    private fun evaluateVarDeclStmt(stmt: VarDeclStmt): Any? {
        val value = evaluateExpression(stmt.expression)
        // In a real interpreter, you'd store this in an environment/symbol table
        // For now, just return the value
        return value
    }

    private fun evaluateBlock(block: Block): Any? {
        var lastValue: Any? = null
        for (stmt in block.stmtList) {
            lastValue = evaluateStatement(stmt)
        }
        return lastValue
    }

    private fun evaluateIfStmt(stmt: IfStmt): Any? {
        val condition = evaluateExpression(stmt.expression)

        return if (isTruthy(condition)) {
            evaluateBlock(stmt.thenBlock)
        } else if (stmt.elseBlock != null) {
            evaluateBlock(stmt.elseBlock)
        } else {
            null
        }
    }

    private fun evaluateDefineStmt(stmt: DefineStmt): Any? {
        // In a real interpreter, you'd create a function object and store it
        // For now, just acknowledge it
        return null
    }

    private fun evaluateExploreStmt(stmt: ExploreStmt): Any? {
        // Evaluate the target
        evaluateExpression(stmt.target)
        // Execute the block
        return evaluateBlock(stmt.block)
    }

    private fun evaluateThrowBallStmt(stmt: ThrowBallStmt): Any? {
        return evaluateExpression(stmt.expression)
    }

    private fun evaluateRunStmt(stmt: RunStmt): Any? {
        // In a real interpreter, this would continue a loop
        return null
    }

    // ----------------------
    // EXPRESSION EVALUATION
    // ----------------------

    private fun evaluateExpression(expr: Expr): Any? {
        return when (expr) {
            is LiteralExpr -> expr.value
            is VariableExpr -> evaluateVariableExpr(expr)
            is UnaryExpr -> evaluateUnaryExpr(expr)
            is BinaryExpr -> evaluateBinaryExpr(expr)
            is AssignExpr -> evaluateAssignExpr(expr)
            is CallExpr -> evaluateCallExpr(expr)
            is PropertyAccessExpr -> evaluatePropertyAccessExpr(expr)
        }
    }

    private fun evaluateVariableExpr(expr: VariableExpr): Any? {
        // In a real interpreter, look up the variable in the environment
        // For now, throw an error
        throw RuntimeError(
            expr.identifier,
            "Undefined variable '${expr.identifier.lexeme}'."
        )
    }

    private fun evaluateUnaryExpr(expr: UnaryExpr): Any? {
        val right = evaluateExpression(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                when (right) {
                    is Double -> -right
                    is Int -> -right
                    else -> 0 // Should never reach here
                }
            }
            TokenType.NOT -> !isTruthy(right)
            else -> throw RuntimeError(
                expr.operator,
                "Unknown unary operator '${expr.operator.lexeme}'."
            )
        }
    }

    private fun evaluateBinaryExpr(expr: BinaryExpr): Any? {
        val left = evaluateExpression(expr.left)
        val right = evaluateExpression(expr.right)

        return when (expr.operator.type) {
            // Arithmetic operators
            TokenType.PLUS -> {
                when {
                    left is Double && right is Double -> left + right
                    left is Int && right is Int -> left + right
                    left is Double && right is Int -> left + right
                    left is Int && right is Double -> left + right
                    left is String && right is String -> left + right
                    else -> throw RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings."
                    )
                }
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Double && right is Double -> left - right
                    left is Int && right is Int -> left - right
                    left is Double && right is Int -> left - right
                    left is Int && right is Double -> left - right
                    else -> 0 // Should never reach here due to check
                }
            }
            TokenType.MULTIPLY -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Double && right is Double -> left * right
                    left is Int && right is Int -> left * right
                    left is Double && right is Int -> left * right
                    left is Int && right is Double -> left * right
                    else -> 0
                }
            }
            TokenType.DIVIDE -> {
                checkNumberOperands(expr.operator, left, right)
                val divisor = when (right) {
                    is Double -> right
                    is Int -> right.toDouble()
                    else -> 0.0
                }
                if (divisor == 0.0) {
                    throw RuntimeError(expr.operator, "Division by zero.")
                }
                when (left) {
                    is Double -> left / divisor
                    is Int -> left / divisor
                    else -> 0.0
                }
            }
            TokenType.MODULO -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Int && right is Int -> {
                        if (right == 0) {
                            throw RuntimeError(expr.operator, "Division by zero.")
                        }
                        left % right
                    }
                    else -> throw RuntimeError(
                        expr.operator,
                        "Modulo operator requires integer operands."
                    )
                }
            }

            // Comparison operators
            TokenType.GREATER_THAN -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) > toDouble(right)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) >= toDouble(right)
            }
            TokenType.LESS_THAN -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) < toDouble(right)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) <= toDouble(right)
            }

            // Equality operators
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.NOT_EQUAL -> !isEqual(left, right)

            // Logical operators
            TokenType.AND -> {
                // Short-circuit evaluation
                if (!isTruthy(left)) {
                    false
                } else {
                    isTruthy(right)
                }
            }
            TokenType.OR -> {
                // Short-circuit evaluation
                if (isTruthy(left)) {
                    true
                } else {
                    isTruthy(right)
                }
            }

            else -> throw RuntimeError(
                expr.operator,
                "Unknown binary operator '${expr.operator.lexeme}'."
            )
        }
    }

    private fun evaluateAssignExpr(expr: AssignExpr): Any? {
        val value = evaluateExpression(expr.value)
        // In a real interpreter, store the value in the environment
        // For now, just return it
        return value
    }

    private fun evaluateCallExpr(expr: CallExpr): Any? {
        // In a real interpreter, evaluate the callee and arguments
        // then invoke the function
        throw RuntimeError(
            Token(TokenType.EOF, "", null, 0),
            "Function calls not yet implemented in evaluator."
        )
    }

    private fun evaluatePropertyAccessExpr(expr: PropertyAccessExpr): Any? {
        // In a real interpreter, evaluate the object and access its property
        throw RuntimeError(
            expr.identifier,
            "Property access not yet implemented in evaluator."
        )
    }

    // ----------------------
    // HELPER FUNCTIONS
    // ----------------------

    /**
     * Determines if a value is "truthy"
     * false and null are falsey, everything else is truthy
     */
    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    /**
     * Checks if two values are equal
     */
    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return true
        if (a == null) return false
        return a == b
    }

    /**
     * Converts a value to a string for printing
     * Made public so Main.kt can use it
     */
    fun stringify(value: Any?): String {
        if (value == null) return "nil"

        // Format numbers nicely
        if (value is Double) {
            val text = value.toString()
            if (text.endsWith(".0")) {
                return text.substring(0, text.length - 2)
            }
            return text
        }

        // Booleans as lowercase
        if (value is Boolean) {
            return value.toString().lowercase()
        }

        return value.toString()
    }

    /**
     * Converts a number to Double for comparison
     */
    private fun toDouble(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Int -> value.toDouble()
            else -> throw IllegalArgumentException("Cannot convert to double")
        }
    }

    /**
     * Type checking for unary operators
     */
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double || operand is Int) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    /**
     * Type checking for binary operators
     */
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if ((left is Double || left is Int) && (right is Double || right is Int)) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }
}