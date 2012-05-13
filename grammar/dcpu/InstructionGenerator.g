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
	import java.util.Map;
	import java.util.ArrayList;
}

@members {
	public List<Instruction> instructions = new ArrayList<Instruction>();
	public List<ReferenceOpNode> references = new ArrayList<ReferenceOpNode>(); 
	public Map<String, Label> labels = null;
	
	public InstructionGenerator(CommonTreeNodeStream nds, Map<String, Label> labels) {
		super(nds);
		this.labels = labels;
	}
	
	private void setLabelIndex(String label, int index) {
		Label l = labels.get(label.toUpperCase());
		if (l == null) {
			System.err.printf("ERROR: trying to set index for label \%s that is undefined\n", label);
		} else {
			l.instructionIndex = index;
		}
	}
	
	private ReferenceOpNode createReference(String name, boolean isMem) {
		if (!labels.containsKey(name.toUpperCase())) {
			System.err.printf("ERROR: Trying to make reference to unknown label: \%s\n", name);
		}
		ReferenceOpNode newRef = new ReferenceOpNode(name.toUpperCase(), isMem);
		references.add(newRef);
		return newRef;
	}
}

program
	:	( i=instruction { if (i != null) instructions.add(i); } )+
	;

instruction returns [Instruction i]
	:	^(OP_BAS OPCODE b=operand a=operand)	{i = new Instruction($OPCODE.text, b, a);}
	|	^(OP_SPE OPCODE a=operand)				{i = new Instruction($OPCODE.text, a);}
	|	^(OP_CMD OPCODE)						{i = new Instruction($OPCODE.text);}
	|	^(DEF LABELDEF)							{setLabelIndex($LABELDEF.text, instructions.size());}
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
	|	^(BR_REG REG ^(REF LABEL))	{n = new RegOpNode($REG.text, $LABEL.text);}
	|	^(BR_SP s=sign e=expr)		{n = new SpOpNode(e, s);}
	|	BR_SP						{n = new SpOpNode();}
	|	^(BR_EXP e=expr)			{n = new ValueOpNode(e, true);}
	|	^(BR_LBL LABEL)				{n = createReference($LABEL.text, true);}
	|	^(REF LABEL)				{n = createReference($LABEL.text, false);}
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
