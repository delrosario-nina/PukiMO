package lexer

enum class TokenType(val symbols: Set<String>? = null) {
    IF_KEYWORD(setOf("if")),
    ELSE_KEYWORD(setOf("else")),
    EXPLORE_KEYWORD(setOf("explore")),
    RUN_KEYWORD(setOf("run")),
    DEFINE_KEYWORD(setOf("define")),
    PRINT_KEYWORD(setOf("print")),
    THROWBALL_KEYWORD(setOf("throwBall")),
    SAFARIZONE_KEYWORD(setOf("SafariZone")),
    TEAM_KEYWORD(setOf("Team")),
    CONST_KEYWORD(setOf("const")),

    NULL_LITERAL(setOf("null")),
    TRUE_LITERAL(setOf("true")),
    FALSE_LITERAL(setOf("false")),

    IDENTIFIER,
    LITERAL, // for numbers and strings
    OPERATOR(setOf("+", "-", "*", "/", "%", "=", "<", ">", "!", "==", "!=", "<=", ">=", "&&", "||")),
    ARROW(setOf("->")),
    DELIMITER(setOf("(", ")", "{", "}", "[", "]", ",")),
    DOT(setOf(".")),
    SEMICOLON(setOf(";")),
    EOF
}