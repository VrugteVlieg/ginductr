/** Mini-Java ANTLR4 grammar **/ //Taken from http://www.cambridge.org/resources/052182060X/MCIIJ2e/grammar.htm
grammar MiniJava;

goal : mainClass classDecl* EOF;
mainClass : CLASS ID LCURL PUBLIC STATIC VOID MAIN LPAR STRING LBRACK RBRACK ID RPAR LCURL statement RCURL RCURL;
classDecl : CLASS ID (EXTENDS ID)? LCURL varDecl* methodDecl* RCURL;
varDecl : TYPE ID SEMICOL;
methodDecl : PUBLIC TYPE ID LPAR (TYPE ID (COMMA TYPE ID)*)?  RPAR LCURL varDecl* statement* RETURN expr SEMICOL RCURL;

statement :   LCURL statement* RCURL
            | IF LPAR expr RPAR statement ELSE statement
            | WHILE LPAR expr RPAR statement
            | PRINT LPAR statement RPAR SEMICOL
            | ID EQ expr SEMICOL
            | ID LBRACK expr RBRACK EQ expr SEMICOL;

expr : expr EXPROP expr
     | expr LBRACK expr RBRACK
     | expr DOT LENGTH 
     | expr DOT ID LPAR (expr (COMMA expr)*)? RPAR
     | NUM
     | TRUE
     | FALSE
     | ID
     | THIS
     | NEW INT LBRACK expr RBRACK
     | NEW ID  LPAR RPAR
     | NOT expr
     | LPAR expr RPAR;

ID : 'java';
TYPE : 'int []' | 'boolean' | INT | ID;
NUM : '0';
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
DOT: '.';
MIN: '-';
NOT: '!';
PLUS: '+';
MUL: '*';
LT: '<';
AND: '&&';
EXPROP : AND | LT | PLUS | MIN | MUL;
PRINT: 'System.out.println';