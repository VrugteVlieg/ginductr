grammar toyExample;
prog : PROGRAM ID EQUALS blck PERIOD;
blck : OPENBRACE _new_ CLOSEBRACE;
ID : LETTER IDLATTER*;
IDLATTER : LETTER | DIGIT | UNDERSCR;
PROGRAM : 'program';
VAR : 'var';
SLEEP : 'sleep';
DIGIT : '0';
LETTER : 'a';
EQUALS : '=';
PLUS : '+';
UNDERSCR : '_';
LBRACK : '(';
RBRACK : ')';
SEMICOLON : ';';
PERIOD : '.';
COLON : ':';
OPENBRACE : '{';
CLOSEBRACE : '}';
BOOL : 'bool';
INT : 'int';
WHILE : 'while';
DO : 'do';
THEN : 'then';
ELSE : 'else';