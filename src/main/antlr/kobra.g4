grammar kobra;

// SECTION: general

program
    : NL* (classDeclaration)* EOF
    ;

declaration
    : classDeclaration
    | functionDeclaration
    ;

functionDeclaration // todo: modifiers
    : FUN simpleIdentifier functionParameters (COLON NL* type)? functionBody
    ;

functionBody
    : block
    | ASSIGNMENT NL* expression
    ;

functionParameters
    : LPAREN (functionParameter (NL* COMMA NL* functionParameter)* (NL* COMMA)?)? NL* RPAREN
    ;

functionParameter
    : identifier COLON identifier
    ;

// SECTION: statements

statement // todo
    : declaration | assignment | expression | returnStatement
    ;

returnStatement
    : RETURN (expression | identifier)
    ;

assignment
    : identifier ASSIGNMENT ( expression | identifier ) NL*
    ;

statements
    : (statement (SEMICOLON? statement)*)? SEMICOLON?
    ;

block
    : LCURL NL* statements NL* RCURL
    ;

// SECTION: classes

classDeclaration
    : CLASS simpleIdentifier (primaryConstructor?)
    (COLON delegationSpecifiers)?
    (NL* classBody)?
    ;

classBody: LCURL NL* classMemberDeclarations NL* RCURL;

classMemberDeclarations: (classMemberDeclaration NL*)*;

classMemberDeclaration: declaration;

primaryConstructor
    : CONSTRUCTOR? classParameters
    ;

classParameters
    : LPAREN NL* (classParameter (NL* COMMA NL* classParameter)* (NL* COMMA)?)? NL* RPAREN
    ;

classParameter
    : (VAL | VAR)? simpleIdentifier COLON NL* type (NL* ASSIGNMENT NL* expression)?
    ;

delegationSpecifiers
    : simpleIdentifier constructorInvocation
    ;

constructorInvocation
    : functionParameters
    ;

// SECTION: expressions

expression // todo
    : simpleIdentifier
    | IntegerLiteral
    | BooleanLiteral
    | NullLiteral
    ;

// SECTION: types

type
    : simpleIdentifier QUEST?
    ;

// SECTION: characters

fragment Letter
    : [a-zA-Z]
    ;

// SECTION: literals

BooleanLiteral: 'true'| 'false';

NullLiteral: 'null';

IntegerLiteral
    : '1'..'9' ('0'..'9')*
    | '0'
    ;

// SECTION: keywords

IMPORT: 'import';
CLASS: 'class';
INTERFACE: 'interface';
FUN: 'fun';
CONSTRUCTOR: 'constructor';
IF: 'if';
ELSE: 'else';
TRY: 'try';
CATCH: 'catch';
FINALLY: 'finally';
FOR: 'for';
RETURN: 'return';

// SECTION: lexicalModifiers

PUBLIC: 'public';
PRIVATE: 'private';
PROTECTED: 'protected';

VAL: 'val';
VAR: 'var';

STRING: '"' (~[\r\n"])* '"';

LPAREN: '(';
RPAREN: ')';
LCURL: '{';
RCURL: '}';
COMMA: ',';
COLON: ':';
ASSIGNMENT: '=';
QUEST: '?';
CONJ: '&&';
DISJ: '||';
SEMICOLON: ';';
DOT: '.';

NL: '\n' | '\r' '\n'?;

LineComment
    : '//' ~[\r\n]*
      -> channel(HIDDEN)
    ;

WS	: 	(' '| '\t') -> skip
	;

// SECTION: lexicalIdentifiers

identifier
    : simpleIdentifier (NL* DOT simpleIdentifier)*
    ;

simpleIdentifier
    : Identifier
    ;

Identifier
    : Letter (Letter | [0-9])*
    | '`' ~([\r\n] | '`')+ '`'
    ;
