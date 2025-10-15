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
data class ExploreStmt(val target: Expr, val block: Block) : Stmt()
data class DefineStmt(val name: Token, val params: List<Token>, val body: Block) : Stmt()
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
data class CallExpr(val callee: Expr, val args: List<Expr>): Expr()
data class PropertyAccessExpr(val obj:Expr, val member:Token):Expr()

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
    private var inControlBlock = 0 // tracks nesting of control statements

    private fun parseStmt(): Stmt {
        return when {
            check(TokenType.IF_KEYWORD) -> parseIfStmt()

            match(TokenType.PRINT_KEYWORD) -> parsePrintStmt()
            check(TokenType.RUN_KEYWORD) -> parseRunStmt()

            check(TokenType.EXPLORE_KEYWORD) ->
                parseExploreStmt()

            check(TokenType.THROWBALL_KEYWORD) ->
               parseThrowBallStmt()

            check(TokenType.DEFINE_KEYWORD) ->
                parseDefineStmt()

            check(TokenType.IDENTIFIER) && peekNext().type == TokenType.ASSIGN -> parseVarDeclStmt()
            match(TokenType.LEFT_BRACE) -> parseBlock()
            else -> parseExprStmt(optionalSemicolon = true)
        }
    }

    private fun parseIfStmt(): Stmt {
        // Consume the 'if' keyword
        val ifToken = consume(TokenType.IF_KEYWORD, "Expected 'if' keyword")

        // Require '(' to start the condition
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'")

        // Parse the condition expression
        val condition = parseExpression()

        // Require ')' to close the condition
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition")

        inControlBlock++

        // Parse the then block, must be a proper block
        val thenBlock = if (check(TokenType.LEFT_BRACE)) {
            parseBlock()
        } else {
            throw error(peek(), "Expected '{' to start 'if' block.")
        }

        // Optional else block
        val elseBlock = if (match(TokenType.ELSE_KEYWORD)) {
            if (check(TokenType.LEFT_BRACE)) {
                parseBlock()
            } else {
                throw error(peek(), "Expected '{' to start 'else' block.")
            }
        } else null

        inControlBlock--
        return IfStmt(condition, thenBlock, elseBlock)
    }

    private fun parseVarDeclStmt(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name.")
        consume(TokenType.ASSIGN, "Expected '=' after variable name.")

        // Parse the right-hand side expression (supports function calls)
        val value = parseExpression()

        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.")
        return VarDeclStmt(name, value)
    }

    private fun parseDefineStmt(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected function name after 'define'")

        // Parse optional parameter list
        val params = mutableListOf<Token>()
        if (match(TokenType.LEFT_PAREN)) {
            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    params.add(consume(TokenType.IDENTIFIER, "Expected parameter name"))
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RIGHT_PAREN, "Expected ')' after function parameters")
        }

        // Parse function body as a block
        val body = parseBlock()

        return DefineStmt(name, params, body)
    }

    private fun parseThrowBallStmt(): Stmt {
        consume(TokenType.THROWBALL_KEYWORD, "Expected 'throwBall' keyword")
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'throwBall'")

        val target = parseExpression() // e.g., 'pokemon'

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
        if (expr is VariableExpr) {
            throw error(expr.name, "Unexpected standalone identifier '${expr.name.lexeme}'.")
        }
        return ExprStmt(expr)
    }


    private fun parseBlock(): Block {
        consume(TokenType.LEFT_BRACE, "Expected '{' at start of block")
        val stmts = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            stmts.add(parseStmt())
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block")
        return Block(stmts)
    }
    private fun parseRunStmt(): Stmt {
        val runToken = consume(TokenType.RUN_KEYWORD, "Expected 'run' keyword") // safely consume it

        if (inControlBlock == 0) {
            throw error(runToken, "'run' can only be used inside a control block")
        }
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
    // EXPRESSION PARSING
    // ----------------------
    private fun parseExpression(): Expr {return parseAssignment()}

    private fun parseAssignment(): Expr {
        val expr = parseOr()
        if (match(TokenType.ASSIGN)) {
            val equals = previous()
            val value = parseAssignment()
            when (expr) {
                is VariableExpr, is PropertyAccessExpr -> return AssignExpr(expr, equals, value)
                else -> throw error(equals, "Invalid assignment target.")
            }
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
        var expr = when {
            match(TokenType.NUMERIC_LITERAL, TokenType.STRING_LITERAL, TokenType.BOOLEAN_LITERAL, TokenType.NULL_LITERAL) ->
                LiteralExpr(previous().literal)
            match(TokenType.IDENTIFIER) ->
                VariableExpr(previous())
            match(TokenType.LEFT_PAREN) -> {
                val innerExpr = parseExpression()
                consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.")
                innerExpr
            }
            else -> throw error(peek(), "Expected expression.")
        }

        while(true) {
            when {
                match(TokenType.DOT) -> {
                    val member = consume(TokenType.IDENTIFIER, "Expected property name after '.'")
                    expr = PropertyAccessExpr(expr, member)
                }
                match(TokenType.ARROW) -> {
                    val method = consume(TokenType.IDENTIFIER, "Expected method name after '->'")
                    val args = mutableListOf<Expr>()
                    if (match(TokenType.LEFT_PAREN)) {
                        if (!check(TokenType.RIGHT_PAREN)) {
                            do {
                                args.add(parseExpression())
                            } while (match(TokenType.COMMA))
                        }
                        consume(TokenType.RIGHT_PAREN, "Expected ')' after method arguments")
                    }
                    expr = CallExpr(PropertyAccessExpr(expr, method), args)
                }
                else -> break
            }
        }

        return expr
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

    private fun error(token: Token, message: String): Exception {
        val location = if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"
        return Exception("[line ${token.lineNumber}] Error at $location: $message")
    }

    fun parseSingleExpr(): Expr? {
        return try {
            val expr = parseExpression()

            if (expr is VariableExpr) {
                throw error(expr.name, "Unexpected standalone identifier '${expr.name.lexeme}'.")
            }

            if (expr is LiteralExpr) {
                throw Exception("Unexpected standalone literal '${expr.value}'.")
            }

            expr
        } catch (e: Exception) {
            println(e.message)
            null
        }
    }

}

