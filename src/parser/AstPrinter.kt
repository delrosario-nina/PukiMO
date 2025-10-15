package parser

class AstPrinter {
    fun print(node: AstNode): String = when (node) {
        is Program -> node.statements.joinToString("\n") { print(it) }
        is VarDeclStmt -> "(var ${node.name.lexeme} = ${print(node.value)})"
        is DefineStmt -> "(define ${node.name.lexeme} = ${print(node.value)})"
        is PrintStmt -> "(print ${print(node.expr)})"
        is ExprStmt -> print(node.expr)
        is IfStmt -> {
            val cond = print(node.condition)
            val thenB = print(node.thenBlock)
            val elseB = node.elseBlock?.let { print(it) } ?: ""
            "(if $cond then $thenB${if (elseB.isNotEmpty()) " else $elseB" else ""})"
        }
        is Block -> node.stmts.joinToString(" ") { print(it) }
        is RunStmt -> "(run ${node.token.lexeme})"
        is ExploreStmt -> "(explore ${node.token.lexeme})"
        is ThrowBallStmt -> "(throw ${print(node.target)})"
        is BinaryExpr -> parenthesize(node.operator.lexeme, node.left, node.right)
        is UnaryExpr -> parenthesize(node.operator.lexeme, node.right)
        is LiteralExpr -> node.value?.toString() ?: "null"
        is VariableExpr -> node.name.lexeme
        is AssignExpr -> "(= ${print(node.target)} ${print(node.value)})"
        else -> "unknown"
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(print(expr))
        }
        builder.append(")")
        return builder.toString()
    }
}