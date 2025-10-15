package parser

class AstPrinter {
    fun print(node: AstNode): String = when (node) {
        is Program -> node.statements.joinToString("\n") { print(it) }
        is VarDeclStmt -> "(var ${node.name.lexeme} = ${print(node.value)})"
        is DefineStmt -> {
            val params = node.params.joinToString(", ") { it.lexeme }
            "(define ${node.name.lexeme}($params) ${print(node.body)})"
        }
        is PrintStmt -> "(print ${print(node.expr)})"
        is ExprStmt -> print(node.expr)
        is IfStmt -> {
            val cond = print(node.condition)
            val thenB = print(node.thenBlock)
            val elseB = node.elseBlock?.let { print(it) } ?: ""
            "(if $cond then $thenB${if (elseB.isNotEmpty()) " else $elseB" else ""})"
        }
        is Block -> node.stmts.joinToString(" ") { print(it) }
        is RunStmt -> "(${node.token.lexeme})"
        is ExploreStmt -> "(explore ${print(node.target)} ${print(node.block)})"
        is ThrowBallStmt -> "throwBall (${print(node.target)})"
        is BinaryExpr -> parenthesize(node.operator.lexeme, node.left, node.right)
        is UnaryExpr -> parenthesize(node.operator.lexeme, node.right)
        is LiteralExpr -> node.value?.toString() ?: "null"
        is VariableExpr -> node.name.lexeme
        is AssignExpr -> "(= ${print(node.target)} ${print(node.value)})"
        is PropertyAccessExpr -> "(${print(node.obj)}.${node.member.lexeme})" // dot remains property
        is CallExpr -> {
            val args = node.args.joinToString(", ") { print(it) }
            // If callee is PropertyAccessExpr and represents a method call, show -> instead of nested ()
            when (node.callee) {
                is PropertyAccessExpr -> "(${print(node.callee.obj)}->${node.callee.member.lexeme}($args))"
                else -> "(${print(node.callee)}($args))"
            }
        }
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