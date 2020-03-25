
%
% Context-free structure
%
prog :- program, id, '=', blck, '.' .

blck :- '{', (decl, ';')*, (stmt, ';')*, '}' .

decl :- var, id, ':', type .

type :- bool | int .

stmt :- (if, expr, then, stmt, (else, stmt)?)
      | (while, expr, do, stmt)
      | (id, '=', expr)
      | sleep 
      | blck .

expr :- (expr, '==', expr) 
      | (expr, '+', expr) 
      | ('(', expr, ')')
      | id
      | num .

%
% Lexical tokens
%
token id :- letter, (letter | digit | '_')* .

fragment letter :-
	'a'|'b'|'c'|'d'|'e'|'f'|'g'|'h'|'i'|'j'|'k'|'l'|'m'|
	'n'|'o'|'p'|'q'|'r'|'s'|'t'|'u'|'v'|'w'|'x'|'y'|'z'|
	'A'|'B'|'C'|'D'|'E'|'F'|'G'|'H'|'I'|'J'|'K'|'L'|'M'|
	'N'|'O'|'P'|'Q'|'R'|'S'|'T'|'U'|'V'|'W'|'X'|'Y'|'Z' .
fragment digit :- '0'|'1'|'2'|'3'|'4'|'3'|'5'|'6'|'7'|'8'|'9' .

token num :- digit+ .
