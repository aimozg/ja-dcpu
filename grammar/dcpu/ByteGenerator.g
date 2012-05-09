tree grammar ByteGenerator;

options {
  language = Java;
  tokenVocab = DCPU;
  ASTLabelType = CommonTree;
}

@header {
	package dcpu;
	import java.util.Map;
	import java.util.HashMap;
	import java.util.Arrays;
	import java.util.LinkedList;
}

@members {
	private AntlrAssembler.AssemblerContext _context;
	private Map<String, Character> labels = new HashMap<String, Character>();
	private char[] bin = new char[128];
	private int counter = 0;
	private void append(char c, int line, boolean code) {
		if (bin.length <= counter) {
			bin = Arrays.copyOf(bin, bin.length * 3 / 2 + 1);
		}

		if (_context.genMap) {
			int lineno = line; // TODO: lineno
			_context.asmmap.binMap.put((char) counter, lineno);
			if (code) _context.asmmap.code.set(counter);
			if (!_context.asmmap.srcMap.containsKey(lineno)) {
				_context.asmmap.srcMap.put(lineno, (char) counter);
			}
		}

		bin[counter++] = c;
	}
    private class Reference {
        final String name;
        final int position;
        final int lineat;

        Reference(String name, int position, int lineat) {
            this.name = name;
            this.position = position;
            this.lineat = lineat;
        }
        public String toString() {
        	return String.format("Reference[name: \%s, position: \%d, lineat: \%d]", name, position, lineat);
        }
    }
    private List<Reference> references = new LinkedList<Reference>();

}

program[AntlrAssembler.AssemblerContext context] returns [char[\] array]
@init {
	_context = $context;
}
	:	i=instruction+
		{
			// back fill the references from the labels
			for (Reference reference : references) {
				Character value = labels.get(reference.name.toLowerCase());
				if (value == null) {
					System.err.println("Unresolved reference name: " + reference.name + " at line: " + reference.lineat);
				} else {
					// System.out.println("setting back ref for value " + (int) value + " at " + reference.position);
					bin[reference.position] = value;
				}
			}
			if (bin.length > counter) {
				bin = Arrays.copyOf(bin, counter);
			}
			array = bin;
		}
	;

instruction
scope {
	List<Integer> nextWords;
}
@init {
	$instruction::nextWords = new ArrayList<Integer>();
}
	:	op_basic
	|	op_special
	|	op_command
	|	data
	|	reserve
	|	label_def
	;

op_basic
	:	^(OP_BASIC o=basic_opcode b=dst_operand a=src_operand)
		{
			Dcpu.BasicOp bop = Dcpu.BasicOp.byName(o.name.toUpperCase());
			if (bop.code < 0 || bop.code > 1 << Dcpu.C_O_BITLEN || 
				       b < 0 ||        b > 1 << Dcpu.C_B_BITLEN || 
				       a < 0 ||        a > 1 << Dcpu.C_A_BITLEN) {
				throw new IllegalArgumentException("Bad arguments: [bop: " + bop + ", a: " + a + ", b: " + b + "]");
			}
			char cmd = (char) (bop.code | a << Dcpu.C_A_SHIFT | b << Dcpu.C_B_SHIFT);
			append(cmd, $OP_BASIC.line, true);
			for (int i=0; i<$instruction::nextWords.size(); i++) {
				append((char) ($instruction::nextWords.get(i) & 0xffff), $OP_BASIC.line, true);
			}
		}
	;

op_special
	:	^(OP_SPECIAL o=special_opcode a=src_operand)
		{
			Dcpu.SpecialOp sop = Dcpu.SpecialOp.byName(o.name.toUpperCase());
			char cmd = (char) (sop.code << Dcpu.C_NBI_O_SHIFT | a << Dcpu.C_NBI_A_SHIFT);
			append(cmd, $OP_SPECIAL.line, true);
			for (int i=0; i<$instruction::nextWords.size(); i++) {
				append((char) ($instruction::nextWords.get(i) & 0xffff), $OP_SPECIAL.line, true);
			}
		}
	;

op_command
	:	^(OP_COMMAND o=command_opcode)
		{
			char cmd = (char) (o << Dcpu.C_NBI_O_SHIFT);
			append(cmd, $OP_COMMAND.line, true);
			// can it push next word values? Doesn't hurt to check in case future commands do
			for (int i=0; i<$instruction::nextWords.size(); i++) {
				append((char) ($instruction::nextWords.get(i) & 0xffff), $OP_COMMAND.line, true);
			}
		}
	;

data
//	:	^(OP_DATA
//			(v=expression { append((char) (v & 0xffff), $OP_DATA.line, false); } )+
//		)
		
	:	^(OP_DATA (d=(STRING|CHAR) | v=expression)+
			{
				// loop through the string data adding chars for each
				if (d != null) {
					String s = d.getText();
					for (int i=0; i<s.length(); i++) {
						append((char) (s.charAt(i) & 0xffff), $OP_DATA.line, false);
					}
				} else {
					append((char) (v & 0xffff), $OP_DATA.line, false);
				}
				
			}
		)
	;

reserve
	:	^(OP_RESERVE n=expression v=expression?)
		{
			for (int i=0; i<n; i++) {
				append((char) (v & 0xffff), $OP_RESERVE.line, false);
			}
		}
	;

label_def
	:	^(LABEL_DEF IDENT)
		{
			// TODO: If the reference already exists, throw an Exception
			if (!labels.containsKey($IDENT.text.toLowerCase())) {
				// System.out.println("creating label " + $IDENT.text.toLowerCase() + " at " + counter); 
				labels.put($IDENT.text.toLowerCase(), (char) (counter & 0xffff));
				
				if (_context.genMap) {
					_context.asmmap.symbolMap.put($IDENT.text.toLowerCase(), (char) (counter & 0xffff));
				}
			} else {
				System.err.println("ERROR Label : " + $IDENT.text.toLowerCase() + " already exists.");
			}
		}
	;

label_ref returns [int value]
	:	^(LABEL_REF IDENT)
		{
			// TODO: This area is part of the optimising logic
			// First version - just use all NW references when doing labels
			$instruction::nextWords.add(0, 0);
			Reference newRef = new Reference($IDENT.text.toLowerCase(), (char) (counter + 1), $IDENT.line);
			references.add(newRef);
			// System.out.printf("Adding reference \%s for '\%s' at \%d\n", newRef, $IDENT.text, counter + 1);
			value = Dcpu.A_NW;
		}
	;

br_label_ref returns [int value]
	:	^(BR_IDENT IDENT r=register?)
		{
			// [ label (+register:r) ]
			// This can never be short, push a reference at current spot and add a NextWord to be filled in
			$instruction::nextWords.add(0, 0);
			Reference newRef = new Reference($IDENT.text.toLowerCase(), (char) (counter + 1), $IDENT.line);
			references.add(newRef);
			if (r == null) {
				value = Dcpu.A_M_NW;
			} else {
				value = Dcpu.A_M_NW_REG | Dcpu.Reg.byName(r.name.toUpperCase()).offset;
			}
		}
	;

operand returns [int value]
	:	r = register
		{
			value = Dcpu.Reg.byName(r.name.toUpperCase()).offset;
		}

	|	^(BR_REG_WITH_EXP sgn=sign r=register e=expression)
		{
			// [register:r +/- value:e]
			// if the sign was negative (as in [A - 1]) then convert to [A + 0xffff]
			if (sgn == -1) {
				e = 0x10000 - e;
			}
			$instruction::nextWords.add(0, e);
			value = Dcpu.A_M_NW_REG | Dcpu.Reg.byName(r.name.toUpperCase()).offset;
		}

	|	^(BR_REG r=register)
		{
			// [register:r]
			value = Dcpu.A_M_REG | Dcpu.Reg.byName(r.name.toUpperCase()).offset;
		}

	|	^(BR_SP_WITH_EXP e=expression)
		{
			// [SP + value:e] == PICK n
			$instruction::nextWords.add(0, e);
			value = Dcpu.A_PICK;
		}

	|	BR_SP
		{
			// [SP] == PEEK
			value = Dcpu.A_PEEK;
		}

	|	^(BR_EXP e = expression)
		{
			// [NW]
			$instruction::nextWords.add(0, e);
			value = Dcpu.A_M_NW;
		}
	
	|	v=br_label_ref { value = v; }

	|	^(PICK e=expression)
		{
			// [SP + value:e] == PICK n
			$instruction::nextWords.add(0, e);
			value = Dcpu.A_PICK;
		}

	|	v=label_ref { value = v; }
	;

sign returns [int s]
	:	PLUS	{ s = 1; }
	|	MINUS	{ s = -1; }
	;

expression returns [int result]
	:	^(PLUS		op1 = expression op2 = expression)	{ result = op1 + op2; }
	|	^(MINUS		op1 = expression op2 = expression)	{ result = op1 - op2; }
	|	^(MULTIPLY	op1 = expression op2 = expression)	{ result = op1 * op2; }
	|	^(DIVIDE	op1 = expression op2 = expression)	{ result = op1 / op2; }
	|	^(NEGATION	e = expression) 					{ result = -e; }
	|	n = number										{ result = n; }
	;

number returns [int result]
	:	e = HEX			{ result = Integer.parseInt(e.getText().substring(2), 16); }
	|	e = BIN			{ result = Integer.parseInt(e.getText().substring(2), 2); }
	|	e = DECIMAL		{ result = Integer.parseInt(e.getText()); }
	;

dst_operand returns [int value]
	:	SP_DEC
		{
			// [--SP] = PUSH
			value = Dcpu.A_PUSHPOP;
		}

	|	c=dst_code 		{ value = c; }

	|	o=operand 		{ value = o; }

	|	e=expression
		{
			// literal values in B are always converted to NW values. Negative values will be converted to chars
			$instruction::nextWords.add(0, e);
			value = Dcpu.A_NW;
		}
	
	;

src_operand returns [int value]
	:	SP_INC
		{
			// [SP++] = POP
			value = Dcpu.A_PUSHPOP;
		}

	|	c = src_code	{ value = c; }

	|	o = operand		{ value = o; }

	|	e=expression
		{
			// values in A can be short.
			// literal value, if it's -1..30 we can decide if a small version can be used
			// if a hex value of 0xffff is given, treat it as -1
			if (e == 0xffff) e = -1;
			if (e >= -1 && e <=30) {
				value = Dcpu.A_CONST + 1 + e;
			} else {
				$instruction::nextWords.add(0, e);
				value = Dcpu.A_NW;
			}
		}
	
	;

basic_opcode returns [String name]
@after { $name = $basic_opcode.start.getText(); }
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

special_opcode returns [String name]
@after { $name = $special_opcode.start.getText(); }
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

command_opcode returns [int value]
	:	HCF				{ value = Dcpu.SpecialOp.HCF.code; }
	|	RFI 			{ value = Dcpu.SpecialOp.RFI.code; }
	;

common_code returns [int value]
	:	PEEK			{ value = Dcpu.A_PEEK; }
	|	SP				{ value = Dcpu.A_SP; }
	|	PC				{ value = Dcpu.A_PC; }
	|	EX				{ value = Dcpu.A_EX; }
	;

dst_code returns [int value]
	:	PUSH			{ value = Dcpu.A_PUSHPOP; }
	|	c=common_code	{ value = c; }
	;

src_code returns [int value]
	:	POP				{ value = Dcpu.A_PUSHPOP; }
	|	c=common_code	{ value = c; }
	;

register returns [String name]
@after { $name = $register.start.getText(); }
	:	REG_A
	|	REG_B
	|	REG_C
	|	REG_I
	|	REG_J
	|	REG_X
	|	REG_Y
	|	REG_Z
	;
