package lexer

import lexer.Token
import lexer.TokenType

class Scanner {

    private var lineNumber: Int = 1

    // helper: check if a symbol belongs to a TokenType
    fun TokenType.contains(symbol: String): Boolean {
        return symbols?.contains(symbol) ?: false
    }

    fun classifyWord(word: String): Pair<TokenType, Any?> {
        return when (word) {
            "if" -> TokenType.IF_KEYWORD to null
            "else" -> TokenType.ELSE_KEYWORD to null
            "explore" -> TokenType.EXPLORE_KEYWORD to null
            "run" -> TokenType.RUN_KEYWORD to null
            "define" -> TokenType.DEFINE_KEYWORD to null
            "print" -> TokenType.PRINT_KEYWORD to null
            "throwBall" -> TokenType.THROWBALL_KEYWORD to null
            "SafariZone" -> TokenType.SAFARIZONE_KEYWORD to null
            "Team" -> TokenType.TEAM_KEYWORD to null
            "const" -> TokenType.CONST_KEYWORD to null

            "null" -> TokenType.NULL_LITERAL to null
            "true" -> TokenType.TRUE_LITERAL to true
            "false" -> TokenType.FALSE_LITERAL to false

            else -> TokenType.IDENTIFIER to null
        }
    }

    // scanners
    fun scanIdentifierOrKeyword(source: String, start: Int): Pair<Token, Int> {
        var index = start
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
        val lexeme = source.substring(start, index)
        val (type, literal) = classifyWord(lexeme)
        return Token(type, lexeme, literal, this.lineNumber) to index
    }

    fun scanNumber(source: String, start: Int): Pair<Token, Int> {
        var index = start
        while (index < source.length && source[index].isDigit()) index++
        val lexeme = source.substring(start, index)
        val literal = lexeme.toIntOrNull()
            ?: throw IllegalArgumentException("Unexpected number '$lexeme'")
        return Token(TokenType.LITERAL, lexeme, literal, this.lineNumber) to index
    }

    fun scanString(source: String, start: Int): Pair<Token, Int> {
        var index = start + 1
        val sb = StringBuilder()

        while (index < source.length && source[index] != '"') {
            val char = source[index]
            if (char == '\n') {
                lineNumber++
                sb.append(char)
                index++
                continue
            }
            if (char == '\\' && index + 1 < source.length) {
                val nextChar = source[index + 1]
                val escaped = when (nextChar) {
                    'n' -> '\n'
                    't' -> '\t'
                    '\\' -> '\\'
                    '"' -> '"'
                    else -> nextChar
                }
                sb.append(escaped)
                index += 2
            } else {
                sb.append(char)
                index++
            }
        }

        if (index >= source.length) throw IllegalArgumentException("Unterminated string")
        index++
        return Token(TokenType.LITERAL, sb.toString(), sb.toString(), lineNumber) to index
    }

    fun scanOperator(source: String, start: Int): Pair<Token, Int> {
        if (start + 1 < source.length) {
            val twoChar = source.substring(start, start + 2)
            if (TokenType.ARROW.contains(twoChar))
                return Token(TokenType.ARROW, twoChar, null, this.lineNumber) to (start + 2)
            if (TokenType.OPERATOR.contains(twoChar))
                return Token(TokenType.OPERATOR, twoChar, null, this.lineNumber) to (start + 2)
        }

        val oneChar = source[start].toString()
        return when {
            TokenType.OPERATOR.contains(oneChar) -> Token(TokenType.OPERATOR, oneChar, null, this.lineNumber) to (start + 1)
            TokenType.DELIMITER.contains(oneChar) -> Token(TokenType.DELIMITER, oneChar, null, this.lineNumber) to (start + 1)
            TokenType.DOT.contains(oneChar) -> Token(TokenType.DOT, oneChar, null, this.lineNumber) to (start + 1)
            TokenType.SEMICOLON.contains(oneChar) -> Token(TokenType.SEMICOLON, oneChar, null, this.lineNumber) to (start + 1)
            else -> throw IllegalArgumentException("Unexpected character '$oneChar' at line $lineNumber")
        }
    }

    fun scanToken(source: String, start: Int): Pair<Token, Int> {
        val char = source[start]
        return when {
            char.isLetter() || char == '_' -> scanIdentifierOrKeyword(source, start)
            char.isDigit() -> scanNumber(source, start)
            char == '"' -> scanString(source, start)
            else -> scanOperator(source, start)
        }
    }

    fun scanLine(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0

        while (index < source.length) {
            val char = source[index]

            if (char.isWhitespace()) { index++; continue }

            if (char == ':' && index + 1 < source.length && source[index + 1] == '>') break

            if (char == '/' && index + 1 < source.length && source[index + 1] == '*') {
                index += 2
                while (index < source.length && !(source[index] == '*' && index + 1 < source.length && source[index + 1] == '/')) index++
                if (index + 1 >= source.length) throw IllegalArgumentException("Unterminated multi-line comment at line $lineNumber")
                index += 2
                continue
            }

            val (token, nextIndex) = scanToken(source, index)
            tokens.add(token)
            index = nextIndex
        }

        tokens.add(Token(TokenType.EOF, "", null, this.lineNumber))
        return tokens
    }
}
