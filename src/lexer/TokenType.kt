package lexer

enum class TokenType(val symbols: Set<String>? = null) {
    KEYWORD(setOf(
        "if", "else", "explore", "run", "define", "print", "throwBall",
        "null", "true", "false", "SafariZone", "Team", "const"
    )),
    IDENTIFIER,
    LITERAL,
    OPERATOR(setOf("+", "-", "*", "/", "%", "=", "<", ">", "!", "==", "!=", "<=", ">=", "&&", "||")),
    ARROW(setOf("->")),
    DELIMITER(setOf("(", ")", "{", "}", "[", "]", ",")),
    DOT(setOf(".")),
    SEMICOLON(setOf(";")),
    EOF
}