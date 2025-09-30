package lexer

fun main() {
    val scanner = Scanner()
    println("Enter multi-line code (type 'exit' on a new line to finish):")

    val code = buildString {
        while (true) {
            val line = readLine() ?: break
            if (line.trim() == "exit") break
            append(line).append("\n")
        }
    }

    try {
        val tokens = scanner.scanLine(code)
        tokens.forEach { println(it) }
    } catch (e: IllegalArgumentException) {
        println("Lexer error: ${e.message}")
    }

    println("Exiting lexer REPL.")
}
