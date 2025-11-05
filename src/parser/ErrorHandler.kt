package parser

import lexer.*

data class ParserError(val token: Token, val message: String, val lineNumber: Int)

class ErrorHandler {
    private val errors = mutableListOf<ParserError>()

    fun error(token: Token, message: String): Exception {
        val location = if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"
        val errorMsg = "$message at $location [line ${token.lineNumber}]"
        val parserError = ParserError(token, errorMsg, token.lineNumber)
        errors.add(parserError)
        return Exception(errorMsg)
    }

    fun getErrors(): List<ParserError> = errors
    fun hasErrors(): Boolean = errors.isNotEmpty()
}
