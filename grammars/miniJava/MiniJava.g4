/** Mini-Java ANTLR4 grammar **/ //Taken from https://github.com/dwysocki/mini-java
grammar MiniJava;


goal
    :   mainClassDeclaration
        classDeclaration*
        EOF
    ;

mainClassDeclaration
    :   CLASS ID
        mainClassBody
    ;

classDeclaration
    :   CLASS ID (EXTENDS type)?
        classBody
    ;

mainClassBody
    :   LCURL mainMethod RCURL
    ;

mainMethod
    :   mainMethodDeclaration LCURL statement RCURL
    ;

mainMethodDeclaration
    :   PUBLIC STATIC VOID MAIN LPAR STRING LBRACK RBRACK ID RPAR
    ;

classBody
    :   LCURL fieldDeclaration*
            methodDeclaration* RCURL
    ;

fieldDeclaration
    :   type ID SEMICOL
    ;

varDeclaration
    :   type ID SEMICOL
    ;

methodDeclaration
    :   ( PUBLIC type ID formalParameters
        |          type ID formalParameters
        | PUBLIC      ID formalParameters
        | PUBLIC type    formalParameters
        | PUBLIC type ID
        )
        methodBody
    ;

methodBody
    :   LCURL
            varDeclaration*
            statement+
        RCURL
    ;

formalParameters
    :   LPAR formalParameterList? RPAR
    ;

formalParameterList
    :   formalParameter (COMMA formalParameter)*
    ;

formalParameter
    :   type ID
    ;

type
    :   intArrayType
    |   BOOLEAN
    |   INT
    |   ID
    ;

statement
    :   LCURL statement* RCURL
    |   IF LPAR expression RPAR
            statement
        ELSE
            statement
    |   WHILE LPAR expression RPAR
            statement
    |   'System.out.println' LPAR expression RPAR SEMICOL
    |   ID EQ expression SEMICOL
    |   ID LBRACK expression RBRACK EQ expression SEMICOL
    |   RETURN expression SEMICOL
    |   RECUR expression QMARK methodArgumentList COLON expression SEMICOL
    ;

expression
    :   expression LBRACK expression RBRACK
    |   expression DOT LENGTH
    |   expression DOT ID methodArgumentList
    |   MIN expression
    |   NOT expression
    |   NEW INT LBRACK expression RBRACK
    |   NEW ID LPAR RPAR
    |   expression PLUS  expression
    |   expression MIN  expression
    |   expression MUL  expression
    |   expression LT  expression
    |   expression AND expression
    |   NUM
    |   bool
    |   ID
    |   THIS
    |   LPAR expression RPAR
    ;

methodArgumentList
    :   LPAR (expression (COMMA expression)*)? RPAR
    ;

intArrayType
    :   INT LBRACK RBRACK
    ;

bool : TRUE | FALSE;


ID  :   'java'
    ;
NUM : '0';
BOOLEAN: 'boolean';
INT: 'int';
CLASS : 'class';
EXTENDS : 'extends';
PUBLIC : 'public';
STATIC : 'static';
VOID : 'void';
MAIN : 'Main';
STRING : 'String';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
RETURN : 'return';
RECUR : 'recur';
LENGTH : 'length';
NEW : 'new';
THIS : 'this';
TRUE : 'true';
FALSE : 'false';
LCURL : '{';
RCURL : '}';
LPAR: '(';
LBRACK: '[';
RBRACK: ']';
RPAR: ')';
SEMICOL: ';';
COMMA: ',';
EQ: '=';
QMARK: '?';
COLON: ':';
DOT: '.';
MIN: '-';
NOT: '!';
PLUS: '+';
MUL: '*';
LT: '<';
AND: '&&';
PRINT: 'System.out.println';