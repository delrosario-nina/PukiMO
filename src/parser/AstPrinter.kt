package parser

class AstPrinter {
    fun print(node: AstNode): String = when (node) {
        is Program -> node.stmtList.joinToString("\n") { print(it) }

        is VarDeclStmt -> "(var ${node.identifier.lexeme} = ${print(node.expression)})"

        is DefineStmt -> {
            val params = node.paramList.joinToString(", ") { it.lexeme }
            "(define ${node.name.lexeme}($params) ${print(node.block)})"
        }

        is PrintStmt -> "(print ${print(node.expression)})"
        is ExprStmt -> print(node.expression)

        is IfStmt -> {
            val cond = print(node.expression)
            val thenB = print(node.thenBlock)
            val elseB = node.elseBlock?.let { print(it) } ?: ""
            "(if $cond then $thenB${if (elseB.isNotEmpty()) " else $elseB" else ""})"
        }

        is Block -> node.stmtList.joinToString(" ") { print(it) }

        is RunStmt -> "(${node.token.lexeme})"

        is ExploreStmt -> "(explore ${print(node.target)} ${print(node.block)})"

        is ThrowBallStmt -> "throwBall (${print(node.expression)})"

        is BinaryExpr -> parenthesize(node.operator.lexeme, node.left, node.right)

        is UnaryExpr -> parenthesize(node.operator.lexeme, node.right)

        is LiteralExpr -> node.value?.toString() ?: "null"

        is VariableExpr -> node.identifier.lexeme

        is AssignExpr -> "(= ${print(node.target)} ${print(node.value)})"

        is PropertyAccessExpr -> "(. ${print(node.primaryWithSuffixes)} ${node.identifier.lexeme})"

        is CallExpr -> {
            val args = node.args.joinToString(" ") { print(it) } // separate args by space
            when (node.callee) {
                is PropertyAccessExpr -> "(->${print(node.callee.primaryWithSuffixes)} ${node.callee.identifier.lexeme}($args))"
                else -> "(${print(node.callee)}($args))"
            }
        }



        else -> "unknown"
    }
    fun printToConsole(node: AstNode) {
        println(print(node))
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ").append(print(expr))
        }
        builder.append(")")
        return builder.toString()
    }
}
