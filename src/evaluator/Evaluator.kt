package evaluator

import lexer.*
import parser.*

// ===== BUILT-IN CONSTRUCTORS =====
object SafariZoneConstructor
object TeamConstructor
object PokemonConstructor

class RuntimeError(val token: Token, message: String) : RuntimeException(message)

class Environment(val parent: Environment? = null) {
    private val variables = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        variables[name] = value
    }

    fun get(name: String): Any? {
        return variables[name] ?: parent?.get(name)
        ?: throw RuntimeException("Undefined variable: $name")
    }

    fun set(name: String, value: Any?) {
        if (variables.containsKey(name)) {
            variables[name] = value
        } else if (parent != null) {
            parent.set(name, value)
        } else {
            variables[name] = value
        }
    }

    fun createChild(): Environment = Environment(this)
}

// ===== POKIMU OBJECTS =====

interface PokemonObject {
    fun getProperty(name: String): Any?
    fun setProperty(name: String, value: Any?)
    fun callMethod(name: String, args: List<Any?>, evaluator: Evaluator): Any?
}

class Pokemon(val name: String, var level: Int = 1, var nature: String = "Neutral") : PokemonObject {
    override fun getProperty(name: String): Any? = when (name) {
        "name" -> this.name
        "level" -> level
        "nature" -> nature
        else -> throw RuntimeException("Unknown property: $name")
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "level" -> level = (value as? Number)?.toInt() ?: level
            "nature" -> nature = value.toString()
            else -> throw RuntimeException("Cannot set property: $name")
        }
    }

    override fun callMethod(name: String, args: List<Any?>, evaluator: Evaluator): Any? {
        return when (name) {
            "evolve" -> { level += 10; this }
            "rename" -> {
                if (args.isNotEmpty()) {
                    throw RuntimeException("Pokemon name is immutable")
                }
                this
            }
            else -> throw RuntimeException("Unknown method: $name")
        }
    }

    override fun toString(): String = "$name (Level $level, $nature)"
}

class Team : PokemonObject {
    private val roster = mutableListOf<Pokemon>()

    override fun getProperty(name: String): Any? = when (name) {
        "pokemon" -> roster
        "size" -> roster.size
        else -> throw RuntimeException("Unknown property: $name")
    }

    override fun setProperty(name: String, value: Any?) {
        throw RuntimeException("Cannot set Team properties directly")
    }

    override fun callMethod(name: String, args: List<Any?>, evaluator: Evaluator): Any? {
        return when (name) {
            "add" -> {
                if (args.isNotEmpty() && args[0] is Pokemon) {
                    roster.add(args[0] as Pokemon)
                    null
                } else throw RuntimeException("add() requires a Pokemon")
            }
            "remove" -> {
                if (args.isNotEmpty() && args[0] is String) {
                    roster.removeIf { it.name == args[0].toString() }
                    null
                } else throw RuntimeException("remove() requires a Pokemon name")
            }
            "find" -> {
                if (args.isNotEmpty()) {
                    val name = args[0].toString()
                    roster.find { it.name == name } ?: throw RuntimeException("Pokemon not found: $name")
                } else throw RuntimeException("find() requires a Pokemon name")
            }
            "filter" -> {
                val filtered = mutableListOf<Pokemon>()
                if (args.isNotEmpty()) {
                    val criterion = args[0].toString()
                    filtered.addAll(roster.filter { it.nature == criterion || it.level.toString() == criterion })
                }
                filtered
            }
            else -> throw RuntimeError(Token(TokenType.EOF, "", null, 0), "Unknown method: $name")
        }
    }

    override fun toString(): String = "Team(${roster.size} pokemon: ${roster.joinToString(", ")})"
}

class SafariZone(var balls: Int, var turns: Int) : PokemonObject {
    private val pokemon = mutableListOf<Pokemon>()

    override fun getProperty(name: String): Any? = when (name) {
        "balls" -> balls
        "turns" -> turns
        "pokemon" -> pokemon
        else -> throw RuntimeException("Unknown property: $name")
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "balls" -> balls = (value as? Number)?.toInt() ?: balls
            "turns" -> turns = (value as? Number)?.toInt() ?: turns
            else -> throw RuntimeException("Cannot set property: $name")
        }
    }

    override fun callMethod(name: String, args: List<Any?>, evaluator: Evaluator): Any? {
        return when (name) {
            "refillBalls" -> {
                if (args.isNotEmpty()) balls += (args[0] as? Number)?.toInt() ?: 0
                null
            }
            "refillTurns" -> {
                if (args.isNotEmpty()) turns += (args[0] as? Number)?.toInt() ?: 0
                null
            }
            else -> throw RuntimeException("Unknown method: $name")
        }
    }

    override fun toString(): String = "SafariZone($balls balls, $turns turns, ${pokemon.size} pokemon)"
}

// ===== FUNCTION OBJECT =====

class FunctionObject(
    val params: List<Token>,
    val body: Block,
    val closure: Environment
)

class Evaluator : AstVisitor<Any?> {
    private var globalEnv = Environment()
    private var currentEnv = globalEnv

    init {
        // Register built-in constructors
        globalEnv.define("SafariZone", SafariZoneConstructor)
        globalEnv.define("Team", TeamConstructor)
        globalEnv.define("Pokemon", PokemonConstructor)
    }

    fun evaluate(node: AstNode): Any? = node.accept(this)

    override fun visitProgram(node: Program): Any? {
        var result: Any? = null
        for (stmt in node.stmtList) {
            result = stmt.accept(this)
        }
        return result
    }

    override fun visitVarDeclStmt(node: VarDeclStmt): Any? {
        val value = node.expression.accept(this)
        currentEnv.define(node.identifier.lexeme, value)
        return null
    }

    override fun visitExprStmt(node: ExprStmt): Any? = node.expression.accept(this)

    override fun visitPrintStmt(node: PrintStmt): Any? {
        val value = node.expression.accept(this)
        println(stringify(value))
        return null
    }

    override fun visitIfStmt(node: IfStmt): Any? {
        val condition = node.expression.accept(this)
        return if (isTruthy(condition)) {
            node.thenBlock.accept(this)
        } else if (node.elseBlock != null) {
            node.elseBlock.accept(this)
        } else {
            null
        }
    }

    override fun visitBlock(node: Block): Any? {
        val previousEnv = currentEnv
        currentEnv = currentEnv.createChild()

        var result: Any? = null
        for (stmt in node.stmtList) {
            result = stmt.accept(this)
        }

        currentEnv = previousEnv
        return result
    }

    override fun visitDefineStmt(node: DefineStmt): Any? {
        val function = FunctionObject(node.paramList, node.block, currentEnv)
        currentEnv.define(node.name.lexeme, function)
        return null
    }

    override fun visitExploreStmt(node: ExploreStmt): Any? {
        val target = node.target.accept(this)
        val iterations = when (target) {
            is Number -> target.toInt()
            else -> throw RuntimeError(Token(TokenType.EOF, "", null, 0), "Explore target must be a number")
        }

        try {
            repeat(iterations) {
                node.block.accept(this)
            }
        } catch (e: BreakException) {
            // Break out of loop
        }
        return null
    }

    override fun visitThrowBallStmt(node: ThrowBallStmt): Any? {
        val value = node.expression.accept(this)
        println("throwBall: $value")
        return null
    }

    override fun visitRunStmt(node: RunStmt): Any? {
        throw BreakException()
    }

    override fun visitLiteralExpr(node: LiteralExpr): Any? = node.value

    override fun visitVariableExpr(node: VariableExpr): Any? {
        return currentEnv.get(node.identifier.lexeme)
    }

    override fun visitAssignExpr(node: AssignExpr): Any? {
        val value = node.value.accept(this)
        when (node.target) {
            is VariableExpr -> currentEnv.set(node.target.identifier.lexeme, value)
            is PropertyAccessExpr -> {
                val obj = node.target.primaryWithSuffixes.accept(this)
                if (obj is PokemonObject) {
                    obj.setProperty(node.target.identifier.lexeme, value)
                } else {
                    throw RuntimeError(Token(TokenType.EOF, "", null, 0), "Cannot set property on non-object")
                }
            }
            else -> throw RuntimeError(Token(TokenType.EOF, "", null, 0), "Invalid assignment target")
        }
        return value
    }

    override fun visitBinaryExpr(node: BinaryExpr): Any? {
        val left = node.left.accept(this)
        val right = node.right.accept(this)

        return when (node.operator.type) {
            TokenType.PLUS -> {
                when {
                    left is String || right is String -> "$left$right"
                    left is Number && right is Number -> toDouble(left) + toDouble(right)
                    else -> throw RuntimeError(node.operator, "Invalid operands for +")
                }
            }
            TokenType.MINUS -> {
                checkNumberOperands(node.operator, left, right)
                toDouble(left) - toDouble(right)
            }
            TokenType.MULTIPLY -> {
                checkNumberOperands(node.operator, left, right)
                toDouble(left) * toDouble(right)
            }
            TokenType.DIVIDE -> {
                checkNumberOperands(node.operator, left, right)
                val divisor = toDouble(right)
                if (divisor == 0.0) throw RuntimeError(node.operator, "Division by zero")
                toDouble(left) / divisor
            }
            TokenType.MODULO -> {
                if (left !is Int || right !is Int) throw RuntimeError(node.operator, "Modulo requires integers")
                left % right
            }
            TokenType.GREATER_THAN -> {
                checkNumberOperands(node.operator, left, right)
                toDouble(left) > toDouble(right)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(node.operator, left, right)
                toDouble(left) >= toDouble(right)
            }
            TokenType.LESS_THAN -> {
                checkNumberOperands(node.operator, left, right)
                toDouble(left) < toDouble(right)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(node.operator, left, right)
                toDouble(left) <= toDouble(right)
            }
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.NOT_EQUAL -> !isEqual(left, right)
            TokenType.AND -> isTruthy(left) && isTruthy(right)
            TokenType.OR -> isTruthy(left) || isTruthy(right)
            else -> throw RuntimeError(node.operator, "Unknown operator")
        }
    }

    override fun visitUnaryExpr(node: UnaryExpr): Any? {
        val right = node.right.accept(this)
        return when (node.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(node.operator, right)
                -toDouble(right)
            }
            TokenType.NOT -> !isTruthy(right)
            else -> throw RuntimeError(node.operator, "Unknown unary operator")
        }
    }

    override fun visitCallExpr(node: CallExpr): Any? {
        val calleeExpr = node.callee

        // Handle method calls (callee is PropertyAccessExpr)
        if (calleeExpr is PropertyAccessExpr) {
            val obj = calleeExpr.primaryWithSuffixes.accept(this)
            val methodName = calleeExpr.identifier.lexeme
            val args = node.args.map { it.accept(this) }

            if (obj is PokemonObject) {
                return obj.callMethod(methodName, args, this)
            } else {
                throw RuntimeError(calleeExpr.identifier, "Cannot call method on non-object")
            }
        }

        // Get the callee
        val callee = calleeExpr.accept(this)
        val args = node.args.map { it.accept(this) }

        return when (callee) {
            // Built-in constructors with argument parsing
            SafariZoneConstructor -> {
                val balls = if (args.isNotEmpty()) (args[0] as? Number)?.toInt() ?: 0 else 0
                val turns = if (args.size > 1) (args[1] as? Number)?.toInt() ?: 0 else 0
                SafariZone(balls, turns)
            }
            TeamConstructor -> Team()
            PokemonConstructor -> {
                if (args.size < 3) throw RuntimeError(Token(TokenType.EOF, "", null, 0), "Pokemon(name, level, nature)")
                val name = args[0].toString()
                val level = (args[1] as? Number)?.toInt() ?: 1
                val nature = args[2].toString()
                Pokemon(name, level, nature)
            }

            // User-defined functions
            is FunctionObject -> {
                val funcEnv = Environment(callee.closure)
                if (args.size != callee.params.size) {
                    throw RuntimeError(Token(TokenType.EOF, "", null, 0),
                        "Expected ${callee.params.size} arguments, got ${args.size}")
                }

                for ((param, arg) in callee.params.zip(args)) {
                    funcEnv.define(param.lexeme, arg)
                }

                val previousEnv = currentEnv
                currentEnv = funcEnv
                val result = callee.body.accept(this)
                currentEnv = previousEnv

                result
            }

            else -> throw RuntimeError(Token(TokenType.EOF, "", null, 0), "Cannot call non-callable")
        }
    }

    override fun visitPropertyAccessExpr(node: PropertyAccessExpr): Any? {
        val obj = node.primaryWithSuffixes.accept(this)

        return when (obj) {
            is PokemonObject -> obj.getProperty(node.identifier.lexeme)
            else -> throw RuntimeError(node.identifier, "Cannot access property on non-object")
        }
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    private fun isEqual(a: Any?, b: Any?): Boolean = a == b

    private fun toDouble(value: Any?): Double = when (value) {
        is Double -> value
        is Int -> value.toDouble()
        else -> throw IllegalArgumentException("Cannot convert to double")
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Number) throw RuntimeError(operator, "Operand must be a number")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left !is Number || right !is Number) {
            throw RuntimeError(operator, "Operands must be numbers")
        }
    }

    fun stringify(value: Any?): String {
        if (value == null) return "nil"
        if (value is Double) {
            val text = value.toString()
            if (text.endsWith(".0")) return text.substring(0, text.length - 2)
            return text
        }
        if (value is Boolean) return value.toString().lowercase()
        if (value is List<*>) return "[${value.joinToString(", ")}]"
        if (value is PokemonObject) return value.toString()
        return value.toString()
    }
}

class BreakException : Exception()
