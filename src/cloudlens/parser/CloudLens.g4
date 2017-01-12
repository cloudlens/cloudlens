grammar CloudLens;

@header{
package cloudlens.parser;

import java.util.Collection;
}

top
	: script
	;

script returns [List<ASTElement> ast]
	: (element)*
	;

element returns [ASTElement ast]
	: declaration
	| block 
	| stream 
	| group  
	| match  
	| lens
	| run
	| restart
	| source
	;
	
declaration returns [ASTDeclaration ast]
	: 'var' varDecl
	| 'function' funDecl
	;

block returns [ASTBlock ast]
	: body
	;
		
stream returns [ASTStream ast]
	: 'process' args conditions body
	| conditions body	
	;

group returns [ASTGroup ast]
	: 'group' args upon output rules 
	;
	
match returns [ASTMatch ast]
	: 'match' args upon rules 
	;
	
lens returns [ASTLens ast]
	: 'lens' IDENT identList lensBody
	;
	
run returns [ASTRun ast]
	: IDENT argList
	;
	
restart returns [ASTRestart ast]
	: 'restart' IDENT
	;
	
source returns [ASTSource ast]
	: 'source' '(' url (',' format (',' path)? )? ')'
	;
		
varDecl returns [ASTDeclaration ast]
	: IDENT '=' ( ~(';') | body | STRING )* ';'
	| IDENT ';'
	;

funDecl returns [ASTDeclaration ast]
	: IDENT identList body
	;
	
	
args returns [ASTArgs ast]
	: '(' IDENT domain ')' 
	| 
	;
		
domain returns [String ast]
	: 'in' IDENT 
	|
	;
	
conditions returns [Collection<Collection<String>> ast]
	: 'when' '(' clause (',' clause)* ')'
	| 
	;

clause returns [Collection<String> ast]
	: (IDENT)+
	;
	
upon returns [String ast]
	: 'upon' '('? IDENT ')'? 
	|
	;
	
output returns [String ast]
	: 'in' '('? IDENT ')'? 
	|
	;
	
body returns [String ast]
	: '{' ( ~('{' | '}') | body | STRING )* '}' 
	;

rules returns [List<String> ast]
	: '{' regex (';' regex)* ';'? '}'
	| '{' '}'
	;
	
regex returns [String ast]
	: STRING
	| IDENT
	| regex '+' regex
	;

identList returns [List<String> ast]
	: '(' IDENT (',' IDENT)* ')'
	| '(' ')'
	;
	
argList returns [String ast]
	: '(' (~(')') | body | STRING)* ')'
	;
	
lensBody returns [List<ASTElement> ast]
	: '{' script '}'
	;

url returns[String ast]
	: STRING
	;
	
format returns[String ast]
	: IDENT
	;
	
path returns[String ast]
	: IDENT
	;

STRING
 	: '\'' ( ESC | ~[\\\r\n'] )* '\''
 	| '"' ( ESC | ~[\\\r\n"] )* '"'
    ;

fragment ESC
	: '\\' .
	;


IDENT
	: WORD ('.' WORD)*
	;
	
fragment WORD
	: ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*
	;
	
COMMENT
	: '//' ~[\r\n]* -> skip
	;
	
MULTILINECOMMENT
 : '/*' .*? '*/' -> skip
 ;	
	
WS
	: [ \t\r\n] -> skip
	;

CHAR
	: .
	;