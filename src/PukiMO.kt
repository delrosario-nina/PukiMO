enum class TokenType {
    KEYWORD, IDENTIFIER, NUMBER, STRING,
    OPERATOR, DELIMITER, DOT, ARROW, SEMICOLON,
    EOF
}

data class Token(val type: TokenType, val lexeme: String, val line: Int)

fun isKeyword(word: String): Boolean {
    return setOf("if", "else", "explore", "run", "define", "print", "throwBall",
        "true", "false", "null", "SafariZone", "Team").contains(word)
}

fun scanWord(source: String, start: Int, line: Int): Pair<Token, Int> {
    var i = start
    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
    val text = source.substring(start, i)
    val type = if (isKeyword(text)) TokenType.KEYWORD else TokenType.IDENTIFIER
    return Token(type, text, line) to i
}

fun scanNumber(source: String, start: Int, line: Int): Pair<Token, Int> {
    var i = start
    while (i < source.length && source[i].isDigit()) i++
    val text = source.substring(start, i)
    return Token(TokenType.NUMBER, text, line) to i
}

fun scanString(source: String, start: Int, line: Int): Pair<Token, Int> {
    var i = start + 1
    val sb = StringBuilder()
    while (i < source.length && source[i] != '"') {
        sb.append(source[i])
        i++
    }
    if (i >= source.length) throw Exception("Unterminated string at line $line")
    i++ // skip closing quote
    return Token(TokenType.STRING, sb.toString(), line) to i
}

fun scanOperator(source: String, start: Int, line: Int): Pair<Token, Int> {
    val twoCharOps = setOf("==", "!=", "<=", ">=", "&&", "||", "->")
    if (start + 1 < source.length) {
        val two = source.substring(start, start + 2)
        if (two in twoCharOps) {
            return Token(if (two == "->") TokenType.ARROW else TokenType.OPERATOR, two, line) to (start + 2)
        }
    }

    val one = source[start].toString()
    val singleOps = setOf("+", "-", "*", "/", "%", "=", "<", ">", "!", "(", ")", "{", "}", "[", "]", ",", ";", ".")
    if (one in singleOps) {
        val type = when (one) {
            ";" -> TokenType.SEMICOLON
            "." -> TokenType.DOT
            "(", ")", "{", "}", "[", "]", "," -> TokenType.DELIMITER
            else -> TokenType.OPERATOR
        }
        return Token(type, one, line) to (start + 1)
    }

    throw Exception("Unexpected character '$one' at line $line")
}

fun scanLine(source: String, line: Int): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    while (i < source.length) {
        val c = source[i]

        if (c.isWhitespace()) { i++; continue }

        // comment check
        if (c == ':' && i + 1 < source.length && source[i + 1] == '>') break
        if (c == '/' && i + 1 < source.length && source[i + 1] == '*') {
            i += 2
            while (i < source.length && !(source[i] == '*' && i + 1 < source.length && source[i + 1] == '/')) i++
            i += 2
            continue
        }

        val (token, next) = when {
            c.isLetter() || c == '_' -> scanWord(source, i, line)
            c.isDigit() -> scanNumber(source, i, line)
            c == '"' -> scanString(source, i, line)
            else -> scanOperator(source, i, line)
        }
        tokens.add(token)
        i = next
    }
    tokens.add(Token(TokenType.EOF, "", line))
    return tokens
}

fun main() {
    println("Enter code (exit to quit):")
    var lineNumber = 1
    while (true) {
        print("> ")
        val input = readLine() ?: break
        if (input.trim() == "exit") break

        try {
            val tokens = scanLine(input, lineNumber)
            tokens.forEach { println(it) }
        } catch (e: Exception) {
            println("Lexer error: ${e.message}")
        }

        lineNumber++
    }
}