package parser

import lexer.Token
import lexer.TokenType.*
import parser.Expr
import parser.Stmt


class Parser(private val tokens: List<Token>) {
    private var current = 0

    // --------------------
    // ENTRY POINT
    // --------------------
    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            statements.add(declaration())
        }
        return statements
    }

    // --------------------
    // DECLARATIONS / STATEMENTS
    // --------------------
    private fun declaration(): Stmt {
        return when {
            match(DEFINE_KEYWORD) -> defineStatement()
            match(EXPLORE_KEYWORD) -> exploreStatement()
            else -> statement()
        }
    }

    private fun statement(): Stmt {
        return when {
            match(IF_KEYWORD) -> ifStatement()
            match(THROWBALL_KEYWORD) -> throwBallStatement()
            match(RUN_KEYWORD) -> runStatement()
            checkArrowCatch() -> catchStatement()
            match(PRINT_KEYWORD) -> printStatement()
            match(LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }
    }

    private fun defineStatement(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect function name after 'define'.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after function name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before function body.")
        val body = block()
        return Stmt.Define(name, parameters, body)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            elseBranch = statement()
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun checkArrowCatch(): Boolean {
        return match(TokenType.ARROW)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(statement())
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun argumentList(): List<Expr> {
        val args = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                args.add(expression())
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
        return args
    }


    // --------------------
    // POKÉMON-SPECIFIC STATEMENTS
    // --------------------
    private fun exploreStatement(): Stmt {
        val expr = if (match(LEFT_PAREN)) {
            val expression = expression()
            consume(RIGHT_PAREN, "Expect ')' after explore expression.")
            expression
        } else null
        val body = Stmt.Block(block())
        return Stmt.Explore(expr, body)
    }

    private fun throwBallStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after throwBall.")
        val args = argumentList()
        consume(RIGHT_PAREN, "Expect ')' after throwBall args.")
        consume(SEMICOLON, "Expect ';' after throwBall().")
        return Stmt.ThrowBall(args)
    }

    private fun runStatement(): Stmt {
        consume(SEMICOLON, "Expect ';' after run.")
        return Stmt.Run()
    }

    private fun catchStatement(): Stmt {
        val left = expression()
        consume(ARROW, "Expect '->' in catch statement.")
        val method = consume(IDENTIFIER, "Expect 'catch' after '->'.")
        consume(SEMICOLON, "Expect ';' after catch statement.")
        return Stmt.Catch(left, method)
    }

    private fun printStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after print.")
        val value = expression()
        consume(RIGHT_PAREN, "Expect ')' after print value.")
        consume(SEMICOLON, "Expect ';' after print.")
        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    // --------------------
    // EXPRESSIONS
    // --------------------
    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = or()
        if (match(ASSIGN)) {
            val equals = previous()
            val value = assignment()
            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }
            throw error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()
        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()
        while (match(EQUAL_EQUAL, NOT_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = term()
        while (match(LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()
        while (match(PLUS, MINUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()
        while (match(MULTIPLY, DIVIDE, MODULO)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr {
        if (match(NOT, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            when {
                match(LEFT_PAREN) -> {
                    val args = argumentList()
                    consume(RIGHT_PAREN, "Expect ')' after arguments.")
                    expr = Expr.Call(expr, args)
                }
                match(DOT) -> {
                    val name = consume(IDENTIFIER, "Expect property name after '.'.")
                    expr = Expr.Get(expr, name)
                }
                match(ARROW) -> {
                    val method = consume(IDENTIFIER, "Expect method name after '->'.")
                    consume(LEFT_PAREN, "Expect '(' after arrow method name.")
                    val args = argumentList()
                    consume(RIGHT_PAREN, "Expect ')' after arrow call.")
                    expr = Expr.ArrowCall(expr, method, args)
                }
                else -> break
            }
        }
        return expr
    }

    private fun primary(): Expr {
        return when {
            match(TRUE_LITERAL) -> Expr.Literal(true)
            match(FALSE_LITERAL) -> Expr.Literal(false)
            match(NULL_LITERAL) -> Expr.Literal(null)
            match(NUMERIC_LITERAL, STRING_LITERAL) -> Expr.Literal(previous().literal)
            match(IDENTIFIER) -> Expr.Variable(previous())
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            else -> throw error(peek(), "Expect expression.")
        }
    }

    // --------------------
    // HELPERS
    // --------------------
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean =
        if (isAtEnd()) false else peek().type == type

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd() = peek().type == EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        System.err.println("[line ${token.lineNumber}] Error at '${token.lexeme}': $message")
        return ParseError()
    }

    private class ParseError : RuntimeException()
}
