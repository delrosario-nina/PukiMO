import lexer.Scanner
import parser.Parser
import parser.AstPrinter

fun main() {
    val scanner = Scanner()
    val printer = AstPrinter()

    println("Enter an expression (type 'exit' to quit):")
    while (true) {
        print("> ")
        val line = readLine() ?: break
        if (line.trim().lowercase() == "exit") break

        val tokens = scanner.scanLine(line)
        val parser = Parser(tokens)
        val expression = parser.parse()

        if (expression != null) {
            println(printer.print(expression))
        }
    }
}
