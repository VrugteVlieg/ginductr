grammar arithmetic;
expression  : term  (Addop term)*;
term        : factor (Mulop factor)* ;
factor      : constant | LPAR  expression RPAR | ;
constant    : Digit Digit* ;
LPAR        :'(';
RPAR        :')';
Digit       : '0';
Mulop       : '*' | '/';
Addop       : '+' | '-';