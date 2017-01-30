grammar CloudLens;

@header{
package cloudlens.parser;

import java.util.Collection;
}

top
	: script
	;

script returns [List<ASTElement> ast]
	: (element (';')*) *
	;

element returns [ASTElement ast]
	: declaration
	| block 
	| process
	| after
	| match  
	| lens
	| run
	| source
	;
	
declaration returns [ASTDeclaration ast]
	: 'var' varDecl
	| 'function' funDecl
	;

block returns [ASTBlock ast]
	: body
	;
		
process returns [ASTProcess ast]
	: 'process' ('(' IDENT ')')? conditions body
	| conditions body	
	;

after returns [ASTAfter ast]
	: 'after' body	
	;

match returns [ASTMatch ast]
	: 'match' ('upon' IDENT)? rules 
	;
	
lens returns [ASTLens ast]
	: 'lens' IDENT identList lensBody
	;
	
run returns [ASTRun ast]
	: IDENT argList
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
		
conditions returns [Collection<Collection<String>> ast]
	: 'when' '(' clause (',' clause)* ')'
	| 
	;

clause returns [Collection<String> ast]
	: (IDENT)+
	;
		
body returns [String ast]
	: '{' ( ~('{' | '}') | body | STRING )* '}' 
	;

rules returns [List<String> ast]
	: '(' regex (';' regex)* ';'? ')'
	| '(' ')'
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