package lexer

enum class TokenType(val symbols: Set<String>? = null) {
        // --------------------
        // Keywords
        // --------------------
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

        // --------------------
        // Literals
        // --------------------
        NULL_LITERAL(setOf("null")),
        TRUE_LITERAL(setOf("true")),
        FALSE_LITERAL(setOf("false")),
        STRING_LITERAL,   // "text" or 'text'
        NUMERIC_LITERAL,  // 123, 45.67, etc.
        BOOLEAN_LITERAL,  // true or false (semantic grouping)
        IDENTIFIER,       // variable names, function names, etc.

        PLUS(setOf("+")),
        MINUS(setOf("-")),
        MULTIPLY(setOf("*")),
        DIVIDE(setOf("/")),
        MODULO(setOf("%")),
        ASSIGN(setOf("=")),
        EQUAL_EQUAL(setOf("==")),
        NOT_EQUAL(setOf("!=")),
        LESS_THAN(setOf("<")),
        GREATER_THAN(setOf(">")),
        LESS_EQUAL(setOf("<=")),
        GREATER_EQUAL(setOf(">=")),
        NOT(setOf("!")),
        AND(setOf("&&")),
        OR(setOf("||")),

        // --------------------
        // Delimiters / Punctuation
        // --------------------
        LEFT_PAREN(setOf("(")),
        RIGHT_PAREN(setOf(")")),
        LEFT_BRACE(setOf("{")),
        RIGHT_BRACE(setOf("}")),
        LEFT_BRACKET(setOf("[")),
        RIGHT_BRACKET(setOf("]")),
        COMMA(setOf(",")),
        DOT(setOf(".")),
        SEMICOLON(setOf(";")),
        ARROW(setOf("->")),

        EOF
}
