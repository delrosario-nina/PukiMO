package parser

import lexer.Scanner
import java.util.Scanner as JavaScanner

fun main() {
    val scanner = Scanner()
    val printer = AstPrinter()
    val input = JavaScanner(System.`in`)
    val buffer = mutableListOf<String>()
    var openBraces = 0

    println("Enter code (type 'exit' to quit):")

    while (true) {
        // Show different prompt if inside a block
        print(if (openBraces > 0) "… " else "> ")

        val line = input.nextLine() ?: break
        val trimmed = line.trim()
        if (trimmed.lowercase() == "exit") break

        // Skip empty lines at top level
        if (trimmed.isEmpty() && openBraces == 0) continue

        // Add line to buffer
        buffer.add(line)

        // Update openBraces count
        openBraces += line.count { it == '{' } - line.count { it == '}' }

        // Only parse when braces are balanced
        if (openBraces <= 0 && buffer.isNotEmpty()) {
            val code = buffer.joinToString("\n")

            try {
                // Scan the entire multi-line code
                val tokens = scanner.scanAll(code)
                val parser = Parser(tokens)

                // Try parsing as a single expression first
                val expr = parser.parse()
                    println(printer.print(expr))


            } catch (e: Exception) {
                println("[line 1] ${e.message ?: "Unknown error"}")
            }

            // Reset buffer and brace count for next input
            buffer.clear()
            openBraces = 0
        }
    }
}