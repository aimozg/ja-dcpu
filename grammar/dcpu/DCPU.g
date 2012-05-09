grammar DCPU;

options {
	language = Java;
	output = AST;
	ASTLabelType = CommonTree;
}

tokens {
	LBRA		= '[';
	RBRA		= ']';
	PLUS		= '+';
	MULTIPLY	= '*';
	DIVIDE		= '/';
	MINUS		= '-';
	LPARENS		= '(';
	RPARENS		= ')';
	DAT			= 'DAT';
	RESERVE		= 'RESERVE';
	SET			= 'SET';
	ADD			= 'ADD';
	SUB			= 'SUB';
	MUL			= 'MUL';
	MLI			= 'MLI';
	DIV			= 'DIV';
	DVI			= 'DVI';
	MOD			= 'MOD';
	MDI			= 'MDI';
	AND			= 'AND';
	BOR			= 'BOR';
	XOR			= 'XOR';
	SHR			= 'SHR';
	ASR			= 'ASR';
	SHL			= 'SHL';
	IFB			= 'IFB';
	IFC			= 'IFC';
	IFE			= 'IFE';
	IFN			= 'IFN';
	IFG			= 'IFG';
	IFA			= 'IFA';
	IFL			= 'IFL';
	IFU			= 'IFU';
	ADX			= 'ADX';
	SBX			= 'SBX';
	STI			= 'STI';
	STD			= 'STD';
	JSR			= 'JSR';
	INT			= 'INT';
	IAG			= 'IAG';
	IAS			= 'IAS';
	RFI			= 'RFI';
	IAQ			= 'IAQ';
	HWN			= 'HWN';
	HWQ			= 'HWQ';
	HWI			= 'HWI';
	RFI			= 'RFI';
	PUSH		= 'PUSH';
	PEEK		= 'PEEK';
	POP			= 'POP';
	SP			= 'SP';
	PC			= 'PC';
	EX			= 'EX';
	PICK		= 'PICK';
	HCF			= 'HCF';
	REG_A		= 'A';
	REG_B		= 'B';
	REG_C		= 'C';
	REG_I		= 'I';
	REG_J		= 'J';
	REG_X		= 'X';
	REG_Y		= 'Y';
	REG_Z		= 'Z';
	
	// marker tokens
	NEGATION;
	LABEL_DEF;
	LABEL_REF;
	OP_BASIC;
	OP_SPECIAL;
	OP_COMMAND;
	OP_DATA;
	OP_RESERVE;
	BR_REG_WITH_EXP;
	BR_REG;
	BR_SP_WITH_EXP;
	BR_SP;
	BR_EXP;
	BR_IDENT;
	SP_DEC;
	SP_INC;
}

@header {
	package dcpu;
}

@lexer::header {
	package dcpu;
}


program
	:	instruction* EOF!
	;

instruction
	:	':' IDENT									-> ^(LABEL_DEF IDENT)
	|	basic_opcode dst_operand ',' src_operand	-> ^(OP_BASIC basic_opcode dst_operand src_operand)
	|	special_opcode src_operand 					-> ^(OP_SPECIAL special_opcode src_operand)
	|	command_opcode								-> ^(OP_COMMAND command_opcode)
	|	RESERVE n=expression (',' v=expression)?
			-> { v == null }?	^(OP_RESERVE $n)
			-> 					^(OP_RESERVE $n $v)
	|	DAT data_values								-> ^(OP_DATA data_values)
	;

operand
	:	register
	|	IDENT				-> ^(LABEL_REF IDENT)
	|	LBRA ( r=register | s=SP ) ( sgn=sign e=expression )? RBRA
			-> { r != null && e != null }? ^(BR_REG_WITH_EXP $sgn $r $e)
			-> { r != null && e == null }? ^(BR_REG $r)
			-> { s != null && e != null }? ^(BR_SP_WITH_EXP $sgn $e)
			-> ^(BR_SP) 
	|	LBRA e=expression ( PLUS ( r=register | s=SP ) )? RBRA
			-> { r != null }? ^(BR_REG_WITH_EXP PLUS $r $e)
			-> { s != null }? ^(BR_SP_WITH_EXP $e)
			-> ^(BR_EXP $e)
	|	LBRA IDENT ( PLUS r=register )? RBRA
			-> { r != null }? ^(BR_IDENT IDENT $r)
			-> ^(BR_IDENT IDENT)
	|	LBRA r=register PLUS IDENT RBRA
			-> ^(BR_IDENT IDENT $r)
	|	PICK^ expression
	|	expression
	;

sign
	:	PLUS
	|	MINUS
	;

dst_operand
	:	LBRA DEC SP RBRA	-> ^(SP_DEC)
	|	dst_code
	|	operand
	;

src_operand
	:	LBRA SP INC RBRA	-> ^(SP_INC)
	|	src_code
	|	operand
	;

data_values
	:	data_item (','! data_item)*
	;

data_item
	:	expression
	|	STRING
	;

///////////////////////////////////////////////////
// expressions

term
	:	LPARENS! expression RPARENS!
	|	literal
	;

unary
	:	(PLUS! | negation^ )* term
	;

negation
	:	'-' -> NEGATION
	;

mult
	:  unary ( ( MULTIPLY^ | DIVIDE^ ) unary )*
	;

expression
	:  mult ( ( PLUS^ | MINUS^ ) mult )*
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

basic_opcode
	:	SET
	|	ADD
	|	SUB
	|	MUL
	|	MLI
	|	DIV
	|	DVI
	|	MOD
	|	MDI
	|	AND
	|	BOR
	|	XOR
	|	SHR
	|	ASR
	|	SHL
	|	IFB
	|	IFC
	|	IFE
	|	IFN
	|	IFG
	|	IFA
	|	IFL
	|	IFU
	|	ADX
	|	SBX
	|	STI
	|	STD
	;

special_opcode
	:	JSR
	|	INT
	|	IAG
	|	IAS
	|	RFI
	|	IAQ
	|	HWN
	|	HWQ
	|	HWI
	|	HCF
	;

command_opcode
	:	HCF
	|	RFI
	;

dst_code
	:	PUSH
	|	PEEK
	|	SP
	|	PC
	|	EX
	;

src_code
	:	POP
	|	PEEK
	|	SP
	|	PC
	|	EX
	;

register
	:	REG_A
	|	REG_B
	|	REG_C
	|	REG_I
	|	REG_J
	|	REG_X
	|	REG_Y
	|	REG_Z
	;

// ------------------------------------------------------------------


INC
	:	'++'
	;

DEC
	:	'--'
	;

HEX
	:	'0' ('x' | 'X') ( 'a'..'f' | 'A'..'F' | DIGIT )+ 
	;

BIN
	:	'0' ('b' | 'B') ('0'|'1')+
	;

DECIMAL
	:	DIGIT+
	;

fragment LETTER
	:	('a'..'z'|'A'..'Z')
	;

fragment DIGIT
	:	'0'..'9'
	;

IDENT
	:	( '.' | LETTER | DIGIT | '_' )+
	;

STRING
	:	'\"' .* '\"' {setText(getText().substring(1, getText().length()-1));}
	;

CHAR
	:	'\'' . '\'' {setText(getText().substring(1, 2));}
	;

COMMENT
    :   ';' ~('\r' | '\n')* { $channel=HIDDEN; }
    ;
   
WS
	:	(' ' | '\n' | '\r' | '\t' | '\f')+ { $channel = HIDDEN; }
	;
