DEFINE enum TokenType:
    - KEYWORD (with reserved words like "if", "else", "explore", "run", "define", "print", "throwBall", "true", "false", "null", "SafariZone", "Team")
    - IDENTIFIER
    - LITERAL
    - OPERATOR (+, -, *, /, ==, etc.)
    - ARROW (->)
    - DELIMITER ((, ), {, }, etc.)
    - DOT (.)
    - SEMICOLON (;)
    - EOF

DEFINE Token as structure with:
    - type (TokenType)
    - lexeme (original text)
    - literal (value if applicable, e.g. number or string)
    - line (line number)

HELPER FUNCTIONS:
    containsSymbol(type, symbol):
        return TRUE if symbol is in type’s symbol set

    isKeyword(text):
        return TRUE if text is in KEYWORD set

SCANNING FUNCTIONS:
    scanWord(source, start, line):
        read letters/digits/underscores
        check if text is a keyword → return KEYWORD token
        else return IDENTIFIER token

    scanNumber(source, start, line):
        read digits
        return LITERAL token with integer value

    scanString(source, start, line):
        read until closing quote "
        allow newlines (increment line counter)
        if missing closing quote → ERROR
        return LITERAL token with string value

    scanOperator(source, start, line):
        first check for two-character operators (==, !=, <=, >=, ->, etc.)
        if not found, check for single-character symbols (+, -, ;, etc.)
        if none match → ERROR
        return appropriate token

    scanToken(source, start, line):
        if starts with letter/underscore → scanWord
        else if starts with digit → scanNumber
        else if starts with " → scanString
        else → scanOperator

SCAN A LINE:
    scanLine(source, line):
        initialize empty token list
    iterate over characters in line:
        skip whitespace
        handle single-line comments (skip rest of line)
        handle multi-line comments (skip until closing */)
        else call scanToken
        add EOF token at the end
        return list of tokens

MAIN PROGRAM (REPL):
    lineNumber = 1
    loop until user types "exit":
        prompt user for input
        if input is "exit" → break
        call scanLine(input, lineNumber)
        print all tokens
        catch and report errors
        increment lineNumber
        print "exit"