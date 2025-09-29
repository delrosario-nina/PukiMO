// token type
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

// token
data class Token(val type: TokenType, val lexeme: String, val literal: Any?, val lineNumber: Int)

// helper functions
// extension function if symbols contain symbol return true otherwise false
fun TokenType.contains(symbol: String): Boolean {
    return symbols?.contains(symbol) ?: false
}

//switch case for null, true, false, and keyword
fun classifyWord(word: String): Pair<TokenType, Any?> {
    return when {
        word == "null" -> TokenType.LITERAL to null
        word == "true" -> TokenType.LITERAL to true
        word == "false" -> TokenType.LITERAL to false
        TokenType.KEYWORD.contains(word) -> TokenType.KEYWORD to null
        else -> TokenType.IDENTIFIER to null
    }
}

// scanners
fun scanIdentifierOrKeyword(source: String, start: Int, lineNumber: Int): Pair<Token, Int> {
    var index = start
    while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
    val lexeme = source.substring(start, index)
    val (type, literal) = classifyWord(lexeme)
    return Token(type, lexeme, literal, lineNumber) to index
}

fun scanNumber(source: String, start: Int, lineNumber: Int): Pair<Token, Int> {
    var index = start
    while (index < source.length && source[index].isDigit()) index++
    val lexeme = source.substring(start, index)
    val literal = lexeme.toIntOrNull()
        ?: throw IllegalArgumentException("Invalid integer '$lexeme' at line $lineNumber")
    return Token(TokenType.LITERAL, lexeme, literal, lineNumber) to index
}

fun scanString(source: String, start: Int, lineNumber: Int): Pair<Token, Int> {
    var index = start + 1
    val sb = StringBuilder()
    var currentLine = lineNumber

    while (index < source.length && source[index] != '"') {
        if (source[index] == '\n') currentLine++
        sb.append(source[index])
        index++
    }
    if (index >= source.length) throw IllegalArgumentException("Unterminated string at line $currentLine")
    index++
    val lexeme = sb.toString()
    return Token(TokenType.LITERAL, lexeme, lexeme, currentLine) to index
}

fun scanOperator(source: String, start: Int, lineNumber: Int): Pair<Token, Int> {
    // Check two-character operators first
    if (start + 1 < source.length) {
        val twoChar = source.substring(start, start + 2)
        if (TokenType.ARROW.contains(twoChar)) return Token(TokenType.ARROW, twoChar, null, lineNumber) to (start + 2)
        if (TokenType.OPERATOR.contains(twoChar)) return Token(TokenType.OPERATOR, twoChar, null, lineNumber) to (start + 2)
    }

    // Check single-character operators or delimiters
    val oneChar = source[start].toString()
    return when {
        TokenType.OPERATOR.contains(oneChar) -> Token(TokenType.OPERATOR, oneChar, null, lineNumber) to (start + 1)
        TokenType.DELIMITER.contains(oneChar) -> Token(TokenType.DELIMITER, oneChar, null, lineNumber) to (start + 1)
        TokenType.DOT.contains(oneChar) -> Token(TokenType.DOT, oneChar, null, lineNumber) to (start + 1)
        TokenType.SEMICOLON.contains(oneChar) -> Token(TokenType.SEMICOLON, oneChar, null, lineNumber) to (start + 1)
        else -> throw IllegalArgumentException("Unexpected character '$oneChar' at line $lineNumber")
    }
}

// main token scanner
fun scanToken(source: String, start: Int, lineNumber: Int): Pair<Token, Int> {
    val char = source[start]
    return when {
        char.isLetter() || char == '_' -> scanIdentifierOrKeyword(source, start, lineNumber)
        char.isDigit() -> scanNumber(source, start, lineNumber)
        char == '"' -> scanString(source, start, lineNumber)
        else -> scanOperator(source, start, lineNumber)
    }
}

// scan line
fun scanLine(source: String, lineNumber: Int): List<Token> {
    val tokens = mutableListOf<Token>()
    var index = 0

    while (index < source.length) {
        val char = source[index]

        // Skip whitespace
        if (char.isWhitespace()) { index++; continue }

        // Single-line comment
        if (char == ':' && index + 1 < source.length && source[index + 1] == '>') break

        // Multi-line comment
        if (char == '/' && index + 1 < source.length && source[index + 1] == '*') {
            index += 2
            while (index < source.length && !(source[index] == '*' && index + 1 < source.length && source[index + 1] == '/')) index++
            if (index + 1 >= source.length) throw IllegalArgumentException("Unterminated multi-line comment at line $lineNumber")
            index += 2
            continue
        }

        val (token, nextIndex) = scanToken(source, index, lineNumber)
        tokens.add(token)
        index = nextIndex
    }

    tokens.add(Token(TokenType.EOF, "", null, lineNumber))
    return tokens
}

//
fun main() {
    println("Enter your code (type 'exit' to quit):")
    var lineNumber = 1
    while (true) {
        print("> ")
        val input = readLine() ?: break
        if (input.trim() == "exit") break
        try {
            val tokens = scanLine(input, lineNumber)
            tokens.forEach { println(it) }
        } catch (e: IllegalArgumentException) {
            println("Lexer error: ${e.message} (Line $lineNumber)")
        }
        lineNumber++
    }
    println("Exiting lexer REPL.")
}
