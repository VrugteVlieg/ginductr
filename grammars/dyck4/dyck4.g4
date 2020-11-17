grammar dyck4;
d : LBRACK d RBRACK d | LPAREN d RPAREN d | LCURL d RCURL d | LT d GT d | ;
LBRACK : '[';
RBRACK : ']';
LPAREN : '(';
RPAREN : ')';
LCURL : '{';
RCURL : '}';
LT : '<';
GT : '>';
