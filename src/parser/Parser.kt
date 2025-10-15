package parser

import lexer.*

sealed class AstNode

data class Program(val statements: List<Stmt>) : AstNode()

// ----------------------
// STATEMENTS
// ----------------------
sealed class Stmt : AstNode()
data class VarDeclStmt(val name: Token, val value: Expr) : Stmt()
data class ExprStmt(val expr: Expr) : Stmt()
data class PrintStmt(val expr: Expr) : Stmt()
data class IfStmt(val condition: Expr, val thenBlock: Block, val elseBlock: Block?) : Stmt()
data class Block(val stmts: List<Stmt>) : Stmt()
data class RunStmt(val token: Token) : Stmt()
data class ExploreStmt(val token: Token) : Stmt()
data class DefineStmt(val name: Token, val value: Expr) : Stmt()
data class ThrowBallStmt(val target: Expr) : Stmt()

// ----------------------
// EXPRESSIONS
// ----------------------
sealed class Expr : AstNode()
data class BinaryExpr(val left: Expr, val operator: Token, val right: Expr) : Expr()
data class UnaryExpr(val operator: Token, val right: Expr) : Expr()
data class LiteralExpr(val value: Any?) : Expr()
data class VariableExpr(val name: Token) : Expr()
data class AssignExpr(val target: Expr, val equals: Token, val value: Expr) : Expr()

// ----------------------
// PARSER CLASS
// ----------------------
class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): Program {
        val stmts = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            stmts.add(parseStmt())
        }
        return Program(stmts)
    }

    // ----------------------
    // STATEMENT PARSING
    // ----------------------
    private fun parseStmt(): Stmt {
        return when {
            match(TokenType.IF_KEYWORD) -> parseIfStmt()
            match(TokenType.PRINT_KEYWORD) -> parsePrintStmt()
            match(TokenType.RUN_KEYWORD) -> RunStmt(previous())
            match(TokenType.EXPLORE_KEYWORD) -> ExploreStmt(previous())
            match(TokenType.THROWBALL_KEYWORD) -> parseThrowBallStmt()
            match(TokenType.DEFINE_KEYWORD) -> parseDefineStmt()
            check(TokenType.IDENTIFIER) && peekNext().type == TokenType.ASSIGN -> parseVarDeclStmt()
            match(TokenType.LEFT_BRACE) -> parseBlock()
            else -> parseExprStmt()
        }
    }

    private fun parseIfStmt(): Stmt {
        val condition = parseExpression()
        val thenBlock = parseBlock()
        val elseBlock = if (match(TokenType.ELSE_KEYWORD)) parseBlock() else null
        return IfStmt(condition, thenBlock, elseBlock)
    }

    private fun parseVarDeclStmt(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name.")
        consume(TokenType.ASSIGN, "Expected '=' after variable name.")
        val expr = parseExpression()
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.")
        return VarDeclStmt(name, expr)
    }

    private fun parseDefineStmt(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected name after 'define'.")
        consume(TokenType.ASSIGN, "Expected '=' after name.")
        val expr = parseExpression()
        consume(TokenType.SEMICOLON, "Expected ';' after definition.")
        return DefineStmt(name, expr)
    }

    private fun parseThrowBallStmt(): Stmt {
        val target = parseExpression()
        consume(TokenType.SEMICOLON, "Expected ';' after 'throw ball' statement.")
        return ThrowBallStmt(target)
    }

    private fun parsePrintStmt(): Stmt {
        val expr = parseExpression()
        consume(TokenType.SEMICOLON, "Expected ';' after print expression.")
        return PrintStmt(expr)
    }

    private fun parseExprStmt(): Stmt {
        val expr = parseExpression()
        consume(TokenType.SEMICOLON, "Expected ';' after expression.")
        return ExprStmt(expr)
    }

    private fun parseBlock(): Block {
        val stmts = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            stmts.add(parseStmt())
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.")
        return Block(stmts)
    }

    // ----------------------
    // EXPRESSION PARSING
    // ----------------------
    private fun parseExpression(): Expr {return parseAssignment()}

    private fun parseAssignment(): Expr {
        val expr = parseOr()
        if (match(TokenType.ASSIGN)) {
            val equals = previous()
            val value = parseAssignment()
            if (expr is VariableExpr) {
                return AssignExpr(expr, equals, value)
            }
            throw error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun parseOr(): Expr {
        var expr = parseAnd()
        while (match(TokenType.OR)) {
            val op = previous()
            val right = parseAnd()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun parseAnd(): Expr {
        var expr = parseEquality()
        while (match(TokenType.AND)) {
            val op = previous()
            val right = parseEquality()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseRelational()
        while (match(TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL)) {
            val op = previous()
            val right = parseRelational()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun parseRelational(): Expr {
        var expr = parseAdditive()
        while (match(TokenType.LESS_THAN, TokenType.LESS_EQUAL, TokenType.GREATER_THAN, TokenType.GREATER_EQUAL)) {
            val op = previous()
            val right = parseAdditive()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun parseAdditive(): Expr {
        var expr = parseMultiplicative()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val op = previous()
            val right = parseMultiplicative()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun parseMultiplicative(): Expr {
        var expr = parseUnary()
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            val op = previous()
            val right = parseUnary()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun parseUnary(): Expr {
        return if (match(TokenType.NOT, TokenType.MINUS)) {
            val op = previous()
            val right = parseUnary()
            UnaryExpr(op, right)
        } else parsePrimary()
    }

    private fun parsePrimary(): Expr {
        return when {
            match(TokenType.NUMERIC_LITERAL, TokenType.STRING_LITERAL, TokenType.BOOLEAN_LITERAL, TokenType.NULL_LITERAL)
                -> LiteralExpr(previous().literal)
            match(TokenType.IDENTIFIER) -> VariableExpr(previous())
            match(TokenType.LEFT_PAREN) -> {
                val expr = parseExpression()
                consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.")
                expr
            }
            else -> throw error(peek(), "Expected expression.")
        }
    }

    // ----------------------
    // UTILITY FUNCTIONS
    // ----------------------
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean =
        !isAtEnd() && peek().type == type

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun peekNext(): Token = tokens.getOrElse(current + 1) { tokens.last() }
    private fun previous(): Token = tokens[current - 1]

    private fun error(token: Token, message: String): Exception =
        Exception("[line ${token.lineNumber}] Error at '${token.lexeme}': $message")

    fun parseSingleExpr(): Expr? {
        return try {
            parseExpression()
        } catch (e: Exception) {
            null
        }
    }
}

