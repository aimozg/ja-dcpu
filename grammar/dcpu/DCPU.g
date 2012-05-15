grammar DCPU;

options {
	language = Java;
	output = AST;
	ASTLabelType = CommonTree;
	backtrack = true;
}

tokens {
	// marker tokens for tree
	NEGATION;
	DEF;
	REF;
	OP_BAS;
	OP_SPE;
	OP_CMD;
	OP_DAT;
	OTHER_OP;
	SP_DEC;
	SP_INC;
	BR_REG;
	BR_SP;
	BR_EXP;
	BR_LBL;
}

@header {
	package dcpu;
	import dcpu.antlr.*;
}

@lexer::header {
	package dcpu;
}

@members {
	LabelTable labelTable;
}

//////////////////////////////////////
// start
//////////////////////////////////////

program[LabelTable labelTable]
@init{this.labelTable = labelTable;}
	:	instruction* EOF!
	;

instruction
	:	LABELDEF						{labelTable.define($LABELDEF.text);}
									-> ^(DEF LABELDEF)
	|	OPCODE dst_op ',' src_op	-> ^(OP_BAS OPCODE dst_op src_op)
	|	OPCODE src_op 				-> ^(OP_SPE OPCODE src_op)
	|	OPCODE						-> ^(OP_CMD OPCODE)
	|	DAT data_values				-> ^(OP_DAT data_values)
	;

operand
	:	REG
	|	PICK^ expr
	|	other_op					-> ^(OTHER_OP other_op)
	|	label						-> ^(REF label)
	|	'[' REG ']'					-> ^(BR_REG REG)
	|	'[' REG sgn expr ']'		-> ^(BR_REG REG sgn expr)
	|	'[' expr '+' REG ']'		-> ^(BR_REG REG '+' expr)
	|	'[' REG '+' label ']'		-> ^(BR_REG REG ^(REF label))
	|	'[' label '+' REG ']'		-> ^(BR_REG REG ^(REF label))
	|	'[' SP sgn expr ']'			-> ^(BR_SP sgn expr)
	|	'[' expr '+' SP ']'			-> ^(BR_SP '+' expr)
	|	'[' SP ']'					-> ^(BR_SP)
	|	'[' expr ']'				-> ^(BR_EXP expr)
	|	'[' label ']'				-> ^(BR_LBL label)
	|	expr
	;

sgn
	:	'+'
	|	'-'
	;

dst_op
	:	'[' SP_MINMIN ']'					-> ^(SP_DEC)
	|	PUSH
	|	operand
	;

src_op
	:	'[' SP_PLUSPLUS ']'					-> ^(SP_INC)
	|	POP
	|	operand
	;

other_op
	:	PEEK
	|	SP
	|	PC
	|	EX
	;

data_values
	:	data_item (','! data_item)*
	;

data_item
	:	expr
	|	STRING
	|	label							-> ^(REF LABEL)
	;


label
	:	LABEL
	|	t=OPCODE {$t.setType(LABEL);}
	;

///////////////////////////////////////////////////
// expressions
///////////////////////////////////////////////////

term
	:	'('! expr ')'!
	|	literal
	;

unary
	:	('+'! | negation^ )* term
	;

negation
	:	'-' -> NEGATION
	;

mult
	:  unary ( ( '*'^ | '/'^ ) unary )*
	;

expr
	:  mult ( ( '+'^ | '-'^ ) mult )*
	;

literal
	:	number
	|	CHAR
	;

number
	:	HEX
	|	BIN
	|	DECIMAL
	;


///////////////////////////////////////////////////
// lex tokens
///////////////////////////////////////////////////

REG: ('A'..'C'|'I'..'J'|'X'..'Z'|'a'..'c'|'i'..'j'|'x'..'z') ;
PUSH: ('P'|'p')('U'|'u')('S'|'s')('H'|'h');
POP: ('P'|'p')('O'|'o')('P'|'p');
PEEK: ('P'|'p')('E'|'e')('E'|'e')('K'|'k');
PICK: ('P'|'p')('I'|'i')('C'|'c')('K'|'k');
SP: ('S'|'s')('P'|'p');
PC: ('P'|'p')('C'|'c');
EX: ('E'|'e')('X'|'x');
DAT: ('D'|'d')('A'|'a')('T'|'t');
OPCODE: LETTER LETTER LETTER;

HEX: '0x' ( 'a'..'f' | 'A'..'F' | DIGIT )+ ;
BIN: '0b' ('0'|'1')+;
DECIMAL: DIGIT+ ;

LABEL: ( '.' | LETTER | DIGIT | '_' )+ ;
LABELDEF: ':' ( '.' | LETTER | DIGIT | '_' )+ {setText(getText().substring(1));} ;

STRING: '\"' .* '\"' {setText(getText().substring(1, getText().length()-1));} ;
CHAR: '\'' . '\'' {setText(getText().substring(1, 2));} ;

COMMENT: ';' ~('\r' | '\n')* { $channel=HIDDEN; } ;
WS: (' ' | '\n' | '\r' | '\t' | '\f')+ { $channel = HIDDEN; } ;

fragment LETTER: ('a'..'z'|'A'..'Z') ;
fragment DIGIT:	'0'..'9' ;
fragment SP_PLUSPLUS: ('S'|'s')('P'|'p')'++';
fragment SP_MINMIN: '--'('S'|'s')('P'|'p');

