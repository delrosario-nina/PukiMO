package lexer
//
fun main() {
    val scanner = Scanner()
    println("Enter multi-line code (type 'exit' to quit):")

    while (true) {
        print("> ")
        val input = readLine() ?: break
        if (input.trim() == "exit") break
        try {
            val tokens = scanner.scanLine(input)
            tokens.forEach { println(it) }
        } catch (e: IllegalArgumentException) {
            println("Lexer error: ${e.message}")
        }
    }
    println("Exiting lexer REPL.")
}

