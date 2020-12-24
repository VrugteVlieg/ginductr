grammar toylang;

program : PROGRAM ID EQ blck DOT;

blck :  LCURL (decl SEMICOLON)* (statement SEMICOLON)* RCURL;

decl : VAR ID COLON type;

type : INT | BOOL;

statement :   IF expr THEN statement (ELSE statement)?
            | WHILE expr DO statement
            | ID EQ expr
            | SLEEP
            | blck;

expr : expr DEQ expr
     | expr PLUS expr
     | LPAR expr RPAR
     | ID
     | NUM;

PROGRAM : 'program';
VAR : 'var';
SLEEP : 'sleep';
NUM : '0';
ID : 'a';
EQ : '=';
DEQ : '==';
PLUS : '+';
LPAR : '(';
RPAR : ')';
SEMICOLON : ';';
DOT : '.';
COLON : ':';
LCURL : '{';
RCURL : '}';
BOOL : 'bool';
INT : 'int';
WHILE : 'while';
DO : 'do';
THEN : 'then';
ELSE : 'else';
IF : 'if';
