grammar VDF;

// starting point
parse
    :   (node | pair)* EOF ;
node
    :   name=STRING (conditional=CONDITIONAL)? '{' (node | pair)* '}' ;
pair
    :   key=STRING (EQUALS)? value=STRING (conditional=CONDITIONAL)? ;

// like an ifdef
CONDITIONAL
    :    '[' .*? ']' ;
// sometimes used in steam .res files between keyvalues
EQUALS
    :   '=' -> channel(HIDDEN) ;
COMMENT
    :   '//' ~[\r\n]* -> channel(HIDDEN) ;
WS
    :   [ \t\r\n\u0000]+ -> channel(HIDDEN) ;
STRING
    :   ('"' STRING_ESCAPED* '"' | STRING_LITERAL) ;
// only quotes are escaped in strings
STRING_ESCAPED
    :   ~["] | '\\"' ;
// an unquoted word
STRING_LITERAL
    :   ~[ \t\r\n\u0000"{}=]+ ;
