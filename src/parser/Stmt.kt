package parser

import lexer.Token

sealed class Stmt {
    data class Expression(val expression: Expr): Stmt()
    data class Print(val expression: Expr): Stmt()
    data class Define(val name: Token, val params: List<Token>, val body: List<Stmt>): Stmt()
    data class Block(val statements: List<Stmt>): Stmt()
    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?): Stmt()
    data class Explore(val target: Expr, val body: List<Stmt>): Stmt()
    data class ThrowBall(val target: Expr): Stmt()
    data class Run(val token: Token): Stmt()
    data class Catch(val pokemon: Expr, val action: Expr?): Stmt()
}
