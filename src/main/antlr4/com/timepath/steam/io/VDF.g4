grammar VDF;

// starting point
parse
    :   (node | pair)* EOF ;
node
    :   name=STRING (QUALIFIER)? '{' (node | pair)* '}' ;
pair
    :   key=STRING (EQUALS)? value=STRING (QUALIFIER)? ;

// like an ifdef
QUALIFIER
    :    '[' .*? ']' ;
// sometimes used in steam .res files between keyvalues
EQUALS
    :   '=' -> channel(HIDDEN) ;
COMMENT
    :   '//' ~[\r\n]* -> channel(HIDDEN) ;
WS
    :   [ \t\r\n]+ -> channel(HIDDEN) ;
STRING
    :   ('"' STRING_ESCAPED* '"' | STRING_LITERAL) ;
// only quotes are escaped in strings
STRING_ESCAPED
    :   ~["] | '\\"' ;
// an unquoted word
STRING_LITERAL
    :   ~[ \t\r\n"{}=]+ ;
