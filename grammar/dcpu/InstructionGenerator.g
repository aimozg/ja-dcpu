tree grammar InstructionGenerator;

options {
	language = Java;
	tokenVocab = DCPU;
	ASTLabelType = CommonTree;
}

@header {
	package dcpu;
	import dcpu.antlr.*;
	import dcpu.antlr.node.*;
}

@members {
	LabelTable labelTable;
	List<Instruction> instructions;
	
	public InstructionGenerator(CommonTreeNodeStream nds, LabelTable labelTable, List<Instruction> instructions) {
		super(nds);
		this.labelTable = labelTable;
		this.instructions = instructions;
	}
}

program
	:	( i=instruction { if (i != null) instructions.add(i); } )+
	;

instruction returns [Instruction i]
	:	^(OP_BAS OPCODE b=operand a=operand)	{i = new Instruction($OPCODE.text, b, a);}
	|	^(OP_SPE OPCODE a=operand)				{i = new Instruction($OPCODE.text, a);}
	|	^(OP_CMD OPCODE)						{i = new Instruction($OPCODE.text);}
	|	^(DEF LABELDEF)							{labelTable.setLabelIndex($LABELDEF.text, instructions.size());}
	|	data
	;

operand returns [OpNode n]
	:	REG							{n = new RegOpNode($REG.text);}
	|	e=expr						{n = new ValueOpNode(e);}
	|	PICK e=expr					{n = new SpOpNode(e, true);}
	|	(SP_DEC|PUSH|SP_INC|POP)	{n = new PushPopOpNode();}
	|	^(OTHER_OP PEEK)			{n = new SpOpNode(true);}
	|	^(OTHER_OP SP)				{n = new SpOpNode();}
	|	^(OTHER_OP PC)				{n = new PcOpNode();}
	|	^(OTHER_OP EX)				{n = new ExOpNode();}
	|	^(BR_REG REG s=sign e=expr)	{n = new RegOpNode($REG.text, e, s);}
	|	^(BR_REG REG)				{n = new RegOpNode($REG.text, true);}
	|	^(BR_REG REG ^(REF LABEL))	{n = labelTable.createReference($REG.text, true, $LABEL.text);}
	|	^(BR_SP s=sign e=expr)		{n = new SpOpNode(e, s);}
	|	BR_SP						{n = new SpOpNode();}
	|	^(BR_EXP e=expr)			{n = new ValueOpNode(e, true);}
	|	^(BR_LBL LABEL)				{n = labelTable.createReference($LABEL.text, true, null);}
	|	^(REF LABEL)				{n = labelTable.createReference($LABEL.text, false, null);}
	;

sign returns [boolean positive]
	:	'+'	{positive = true;}
	|	'-'	{positive = false;}
	;

// TODO: data
data
	:	^(OP_DAT
			(
				d=(STRING|CHAR)
				| v=expr
				| ^(REF LABEL)
			)+
		)
	;

expr returns [int result]
	:	^('+'		op1 = expr op2 = expr)	{ result = op1 + op2; }
	|	^('-'		op1 = expr op2 = expr)	{ result = op1 - op2; }
	|	^('*'		op1 = expr op2 = expr)	{ result = op1 * op2; }
	|	^('/'		op1 = expr op2 = expr)	{ result = op1 / op2; }
	|	^(NEGATION	  e = expr) 			{ result = -e; }
	|	n = number							{ result = n; }
	;

number returns [int result]
	:	e = HEX			{ result = Integer.parseInt(e.getText().substring(2), 16); }
	|	e = BIN			{ result = Integer.parseInt(e.getText().substring(2), 2); }
	|	e = DECIMAL		{ result = Integer.parseInt(e.getText()); }
	;
