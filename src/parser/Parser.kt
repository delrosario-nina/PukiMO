package parser

import lexer.*

sealed class AstNode

data class Program(val stmtList: List<Stmt>) : AstNode()

// ----------------------
// STATEMENTS
// ----------------------
sealed class Stmt : AstNode()

data class IfStmt(val expression: Expr, val thenBlock: Block, val elseBlock: Block?) : Stmt()
data class VarDeclStmt(val identifier: Token, val expression: Expr) : Stmt()
data class ExprStmt(val expression: Expr) : Stmt()
data class PrintStmt(val expression: Expr) : Stmt()
data class Block(val stmtList: List<Stmt>) : Stmt()
data class RunStmt(val token: Token) : Stmt()
data class ExploreStmt(val target: Expr, val block: Block) : Stmt()
data class DefineStmt(val name: Token, val paramList: List<Token>, val block: Block) : Stmt()
data class ThrowBallStmt(val expression: Expr) : Stmt()

// ----------------------
// EXPRESSIONS
// ----------------------
sealed class Expr : AstNode()
data class BinaryExpr(val left: Expr, val operator: Token, val right: Expr) : Expr()
data class UnaryExpr(val operator: Token, val right: Expr) : Expr()
data class LiteralExpr(val value: Any?) : Expr()
data class VariableExpr(val identifier: Token) : Expr()
data class AssignExpr(val target: Expr, val equals: Token, val value: Expr) : Expr()
data class CallExpr(val callee: Expr, val args: List<Expr>) : Expr()
data class PropertyAccessExpr(val primaryWithSuffixes: Expr, val identifier: Token) : Expr()

// ----------------------
// PARSER CLASS
// ----------------------
class Parser(private val tokens: List<Token>) {
    private var current = 0
    private var inControlBlock = 0

    // ----------------------
    // ENTRY POINT
    // ----------------------
    fun parse(): Program {
        val stmtList = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            stmtList.add(
                if (check(TokenType.IF_KEYWORD)) parseIfStmt()
                else parseNonIfStmt()
            )
        }
        return Program(stmtList)
    }

    // ----------------------
    // STATEMENT PARSING
    // ----------------------
    private fun parseNonIfStmt(): Stmt {
        return when {
            check(TokenType.IDENTIFIER) && peekNext().type == TokenType.ASSIGN -> parseVarDeclStmt()
            match(TokenType.PRINT_KEYWORD) -> parsePrintStmt()
            check(TokenType.RUN_KEYWORD) -> parseRunStmt()
            match(TokenType.EXPLORE_KEYWORD) -> parseExploreStmt()
            check(TokenType.THROWBALL_KEYWORD) -> parseThrowBallStmt()
            check(TokenType.DEFINE_KEYWORD) -> parseDefineStmt()
            match(TokenType.LEFT_BRACE) -> parseBlock()
            else -> parseExprStmt(optionalSemicolon = true)
        }
    }

    private fun parseIfStmt(): Stmt {
        consume(TokenType.IF_KEYWORD, "Expected 'if' keyword")
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'")
        val condition = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition")

        inControlBlock++
        val thenBlock = if (check(TokenType.LEFT_BRACE)) parseBlock()
        else throw error(peek(), "Expected '{' to start 'if' block.")

        val elseBlock = if (match(TokenType.ELSE_KEYWORD)) {
            if (check(TokenType.LEFT_BRACE)) parseBlock()
            else throw error(peek(), "Expected '{' to start 'else' block.")
        } else null
        inControlBlock--

        return IfStmt(condition, thenBlock, elseBlock)
    }

    private fun parseVarDeclStmt(): Stmt {
        val identifier = consume(TokenType.IDENTIFIER, "Expected variable name.")
        consume(TokenType.ASSIGN, "Expected '=' after variable name.")
        val expr = parseExpression()
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.")
        return VarDeclStmt(identifier, expr)
    }

    private fun parseDefineStmt(): Stmt {
        consume(TokenType.DEFINE_KEYWORD, "Expected 'define' keyword")
        val name = consume(TokenType.IDENTIFIER, "Expected function name after 'define'")
        val params = mutableListOf<Token>()
        if (match(TokenType.LEFT_PAREN)) {
            if (!check(TokenType.RIGHT_PAREN)) {
                do { params.add(consume(TokenType.IDENTIFIER, "Expected parameter name")) }
                while (match(TokenType.COMMA))
            }
            consume(TokenType.RIGHT_PAREN, "Expected ')' after function parameters")
        }
        val body = parseBlock()
        return DefineStmt(name, params, body)
    }

    private fun parseThrowBallStmt(): Stmt {
        consume(TokenType.THROWBALL_KEYWORD, "Expected 'throwBall' keyword")
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'throwBall'")
        val target = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after throwBall target")
        consume(TokenType.SEMICOLON, "Expected ';' after throwBall statement.")
        return ThrowBallStmt(target)
    }

    private fun parsePrintStmt(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'print'")
        val expr = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after print expression")
        consume(TokenType.SEMICOLON, "Expected ';' after print statement")
        return PrintStmt(expr)
    }

    private fun parseExprStmt(optionalSemicolon: Boolean = false): Stmt {
        val expr = parseExpression()
        if (!optionalSemicolon || check(TokenType.SEMICOLON)) {
            consume(TokenType.SEMICOLON, "Expected ';' after expression.")
        }
        if (expr is VariableExpr) throw error(expr.identifier, "Unexpected standalone identifier '${expr.identifier.lexeme}'.")
        return ExprStmt(expr)
    }

    private fun parseBlock(): Block {
        consume(TokenType.LEFT_BRACE, "Expected '{' at start of block")
        val stmts = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            stmts.add(parseNonIfStmt())
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block")
        return Block(stmts)
    }

    private fun parseRunStmt(): Stmt {
        val runToken = consume(TokenType.RUN_KEYWORD, "Expected 'run' keyword")
        if (inControlBlock == 0) throw error(runToken, "'run' can only be used inside a control block")
        consume(TokenType.SEMICOLON, "Expected ';' after 'run' statement.")
        return RunStmt(runToken)
    }

    private fun parseExploreStmt(): Stmt {
        val target = parseExpression()
        inControlBlock++
        val block = parseBlock()
        inControlBlock--
        return ExploreStmt(target, block)
    }

    // ----------------------
    // EXPRESSION PARSING (CFG-COMPLIANT)
    // ----------------------
    private fun parseExpression(): Expr = parseAssignExpr()

    private fun parseAssignExpr(): Expr {
        val expr = parseOr()
        if (match(TokenType.ASSIGN)) {
            val equals = previous()
            val value = parseAssignExpr()
            if (expr is VariableExpr || expr is PropertyAccessExpr) return AssignExpr(expr, equals, value)
            else throw error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun parseOr(): Expr {
        var expr = parseAnd()
        while (match(TokenType.OR)) expr = BinaryExpr(expr, previous(), parseAnd())
        return expr
    }

    private fun parseAnd(): Expr {
        var expr = parseEquality()
        while (match(TokenType.AND)) expr = BinaryExpr(expr, previous(), parseEquality())
        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseRelational()
        while (match(TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL)) expr = BinaryExpr(expr, previous(), parseRelational())
        return expr
    }

    private fun parseRelational(): Expr {
        var expr = parseAdditive()
        while (match(TokenType.LESS_THAN, TokenType.LESS_EQUAL, TokenType.GREATER_THAN, TokenType.GREATER_EQUAL)) expr = BinaryExpr(expr, previous(), parseAdditive())
        return expr
    }

    private fun parseAdditive(): Expr {
        var expr = parseMultiplicative()
        while (match(TokenType.PLUS, TokenType.MINUS)) expr = BinaryExpr(expr, previous(), parseMultiplicative())
        return expr
    }

    private fun parseMultiplicative(): Expr {
        var expr = parseUnary()
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) expr = BinaryExpr(expr, previous(), parseUnary())
        return expr
    }

    private fun parseUnary(): Expr {
        return if (match(TokenType.NOT, TokenType.MINUS)) UnaryExpr(previous(), parseUnary())
        else parsePrimaryWithSuffixes()
    }

    private fun parsePrimaryWithSuffixes(): Expr {
        var expr = parsePrimary()
        while (true) {
            expr = when {
                match(TokenType.DOT) -> {
                    val member = consume(TokenType.IDENTIFIER, "Expected property name after '.'")
                    PropertyAccessExpr(expr, member)
                }

                match(TokenType.ARROW) -> {
                    val member = consume(TokenType.IDENTIFIER, "Expected method name after '->'")
                    if (match(TokenType.LEFT_PAREN)) {
                        val args = parseArgList()
                        CallExpr(PropertyAccessExpr(expr, member), args)
                    } else PropertyAccessExpr(expr, member)
                }

                match(TokenType.LEFT_PAREN) -> {
                    val args = parseArgList()
                    CallExpr(expr, args)
                }

                else -> break
            }
        }
        return expr
    }

    private fun parsePrimary(): Expr {
        return when {
            match(TokenType.NUMERIC_LITERAL, TokenType.STRING_LITERAL, TokenType.BOOLEAN_LITERAL, TokenType.NULL_LITERAL) -> LiteralExpr(previous().literal)
            match(TokenType.IDENTIFIER) -> VariableExpr(previous())
            match(TokenType.SAFARI_ZONE, TokenType.TEAM) -> {
                val token = previous()
                consume(TokenType.LEFT_PAREN, "Expected '(' after ${token.lexeme}")
                val args = parseArgList()
                CallExpr(VariableExpr(token), args)
            }
            match(TokenType.LEFT_PAREN) -> {
                val expr = parseExpression()
                consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
                expr
            }
            else -> throw error(peek(), "Expected primary expression")
        }
    }

    private fun parseArgList(): List<Expr> {
        val args = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do args.add(parseExpression()) while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments")
        return args
    }

    // ----------------------
    // UTILITY FUNCTIONS
    // ----------------------
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) if (check(type)) { advance(); return true }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type
    private fun advance(): Token { if (!isAtEnd()) current++; return previous() }
    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun peekNext(): Token = tokens.getOrElse(current + 1) { tokens.last() }
    private fun previous(): Token = tokens[current - 1]
    private fun error(token: Token, message: String): Exception {
        val location = if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"
        return Exception("[line ${token.lineNumber}] Error at $location: $message")
    }

}
