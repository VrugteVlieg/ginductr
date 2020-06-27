grammar sletoy;

prog 
     : 'program'  ID '=' block '.' EOF 
     ;

block 
    :  '{' (decl ';')* (stmt ';')* '}'
    ;

decl 
    :   'var' ID ':' type 
    ;

type 
    :  'bool' | 'int'
    ;

stmt 
    : 'sleep'
    | 'if' expr 'then' stmt ('else' stmt)?
    | 'while' expr 'do' stmt 
    |  ID '=' expr
    |  block
    ;

expr
    : term expr_list
    ;
expr_list 
    : '=' factor expr_list 
    | 
    ;
term 
    : factor term_list
    ;

term_list 
    : '+' factor term_list  
    |
    ;
factor 
    : '(' expr ')' 
    | ID 
    | NUM
    ;

ID :  [a-zA-Z][a-zA-Z0-9_]*
   ;

NUM  : [0-9]+
     ;

WS  : [ \n\r\t]+ -> skip
    ;

