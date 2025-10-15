package parser

import lexer.Scanner
import java.util.Scanner as JavaScanner
import parser.Parser

fun main() {
    val scanner = Scanner()
    val printer = AstPrinter()

    println("Enter an expression (type 'exit' to quit):")

    val input = JavaScanner(System.`in`)
    while (true) {
        print("> ")
        val line = input.nextLine() ?: break
        if (line.trim().lowercase() == "exit") break
        if (line.trim().isEmpty()) continue

        try {
            val tokens = scanner.scanLine(line)
            val parser = Parser(tokens)
            val expression = parser.parseSingleExpr()

            if (expression != null) {
                println(printer.print(expression))
            } else {
                val program = parser.parse()
                println(printer.print(program))
            }

        } catch (e: Exception) {
            println("[line 1] ${e.message ?: "Unknown error"}")
        }
    }
}
