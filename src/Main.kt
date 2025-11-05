package parser

import lexer.Scanner
import evaluator.Evaluator
import java.util.Scanner as JavaScanner

fun main() {
    val scanner = Scanner()
    val input = JavaScanner(System.`in`)
    val buffer = mutableListOf<String>()
    var openBraces = 0
    val printer = AstPrinter()
    val evaluator = Evaluator()  // ✅ Add this

    println("Enter code (type 'exit' to quit):")

    while (true) {
        print(if (openBraces > 0) "… " else "> ")

        val line = input.nextLine() ?: break
        val trimmed = line.trim()
        if (trimmed.lowercase() == "exit") break

        if (trimmed.isEmpty() && openBraces == 0) continue

        buffer.add(line)
        openBraces += line.count { it == '{' } - line.count { it == '}' }

        if (openBraces <= 0 && buffer.isNotEmpty()) {
            val code = buffer.joinToString("\n")

            try {
                val tokens = scanner.scanAll(code)
                val parser = Parser(tokens)
                val ast = parser.parse()


                try {
                    val result = evaluator.evaluate(ast)
                    if (result != null) println(result)
                } catch (e: Exception) {
                    println("Runtime Error: ${e.message}")
                }


            } catch (e: Exception) {
                println(e.message ?: "Unknown error")
            }

            buffer.clear()
            openBraces = 0
        }
    }
}
