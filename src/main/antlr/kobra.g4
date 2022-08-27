grammar kobra;

// SECTION: general

program
    : NL* (declaration NL*)* EOF
    ;

declaration
    : classDeclaration
    | functionDeclaration
    | propertyDeclaration
    ;

propertyDeclaration
    : (VAL | VAR) simpleIdentifier (COLON NL* type)? (NL* (ASSIGNMENT NL* expression))?
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
    : delegationSpecifier (NL* COMMA NL* delegationSpecifier)*
    ;

delegationSpecifier
    : simpleIdentifier constructorInvocation
    ;

constructorInvocation
    : functionParameters
    ;

// SECTION: expressions

expression
    : disjunction
    ;

disjunction
    : conjunction (NL* DISJ NL* conjunction)*
    ;

conjunction
    : equality (NL* CONJ NL* equality)*
    ;

equality
    : comparison (equalityOperator NL* comparison)*
    ;

comparison
    : genericCallLikeComparison (comparisonOperator NL* genericCallLikeComparison)*
    ;

genericCallLikeComparison
    : infixOperation
    ;

infixOperation
    : elvisExpression (inOperator NL* elvisExpression | isOperator NL* type)*
    ;

elvisExpression
    : infixFunctionCall (NL* elvis NL* infixFunctionCall)*
    ;

infixFunctionCall
    : rangeExpression (simpleIdentifier NL* rangeExpression)*
    ;

rangeExpression
    : additiveExpression (RANGE NL* additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression (additiveOperator NL* multiplicativeExpression)*
    ;

multiplicativeExpression
    : asExpression (multiplicativeOperator NL* asExpression)*
    ;

asExpression
    : prefixUnaryExpression (NL* asOperator NL* type)*
    ;

prefixUnaryExpression
    : unaryPrefix* postfixUnaryExpression
    ;

elvis
    : QUEST_NO_WS COLON
    ;

equalityOperator
    : EXCL_EQ
    | EQEQ
    ;

comparisonOperator
    : LANGLE
    | RANGLE
    | LE
    | GE
    ;

inOperator
    : IN
    | NOT_IN
    ;

isOperator
    : IS
    | NOT_IS
    ;

asOperator
    : AS
    | AS_SAFE
    ;

additiveOperator
    : ADD
    | SUB
    ;

multiplicativeOperator
    : MULT
    | DIV
    | MOD
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

StringLiteral
    : STRING
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
IS: 'is';
IN: 'in';
NOT_IS: '!is' (Hidden | NL);
NOT_IN: '!in' (Hidden | NL);
AS: 'as';
AS_SAFE: 'as?';

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
QUEST: '?' Hidden;
CONJ: '&&';
DISJ: '||';
SEMICOLON: ';';
DOT: '.';
QUOTE: '"';
EQEQ: '==';
EXCL_EQ: '!=';
LANGLE: '<';
RANGLE: '>';
LE: '<=';
GE: '>=';
QUEST_NO_WS: '?';
ADD: '+';
SUB: '-';
MULT: '*';
DIV: '/';
MOD: '%';

NL: '\n' | '\r' '\n'?;

fragment Hidden: DelimitedComment | LineComment | WS;


LineComment
    : '//' ~[\r\n]*
      -> channel(HIDDEN)
    ;

DelimitedComment
    : '/*' ( DelimitedComment | . )*? '*/'
      -> channel(HIDDEN)
    ;

WS	: 	(' '| '\t' | '\u000C') -> skip
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
