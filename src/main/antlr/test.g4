grammar test;

program
	:	statementList
	;

statementList
	:	statement*
	;

statement
	:	printStatement
	;

printStatement
	: 	'print' LPARENT STRING RPARENT ';'
	;


STRING
	: '"' (~[\r\n"])* '"'
	;

LPARENT
    : '('
    ;

RPARENT
    : ')'
    ;

WS	: 	(' '| '\t' | '\n' | '\r') -> skip
	;
