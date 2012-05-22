package test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import dcpu.AntlrAssembler;
import dcpu.AntlrAssembler.AssemblerContext;
import dcpu.DCPULexer;
import dcpu.DCPUParser;
import dcpu.DCPUParser.program_return;
import dcpu.Dcpu;
import dcpu.InstructionGenerator;
import dcpu.antlr.Instruction;
import dcpu.antlr.Label;
import dcpu.antlr.LabelTable;
import dcpu.antlr.node.ReferenceOpNode;

public class TestAssemblerGrammar {

	public static void main(String[] args) throws RecognitionException, IOException {
	    LabelTable labelTable = new LabelTable();
	    List<Instruction> instructions = new LinkedList<Instruction>();

	    CharStream charStream = new ANTLRReaderStream(new InputStreamReader(TestAssemblerGrammar.class.getResourceAsStream("Test.asm")));
		DCPULexer lexer = new DCPULexer(charStream);
		
		TokenStream tokenStream = new CommonTokenStream(lexer);
		DCPUParser parser = new DCPUParser(tokenStream);
		program_return program = parser.program(labelTable);
		
		CommonTree tree = (CommonTree) program.getTree();
        System.out.println("tree:\n" + tree.toStringTree());

        CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tree);
		
        InstructionGenerator gen = new InstructionGenerator(nodeStream, labelTable, instructions);
		AntlrAssembler.AssemblerContext context = new AssemblerContext();
		context.useShortLiterals = true;
		
		gen.program();

		System.out.println("LABELS:");
		System.out.println(labelTable.labels.values());

		// check for any illegal labels (only trailing references count)
		for (Entry<String, Label> entry : labelTable.labels.entrySet()) {
            Label label = entry.getValue();
            if (label.instructionIndex >= instructions.size()) {
                System.err.printf("ERROR: label %s refers to unknown instruction\n", label.name);
            }
        }
		
		List<ReferenceOpNode> references = labelTable.references;
		// walk the instructions, fill in their address and lengths if we can
		Instruction previous = null;
		for (Instruction instruction : instructions) {
			List<Integer> nextWords = new ArrayList<Integer>();
			char code;
			int aCode;
			int bCode;
			switch (instruction.type) {
			case BASIC:
				if (!instruction.dstIsReference() && !instruction.srcIsReference()) {
					bCode = instruction.dst.evaluate(nextWords);
					aCode = instruction.src.evaluate(nextWords);
				} else {
					// at least one reference
					bCode = 0;
					aCode = 0;
				}
				code = Dcpu.gencmd(Dcpu.BasicOp.byName(instruction.name).code, bCode, aCode);
				instruction.setBin(code, nextWords);
				setAddress(instruction, previous);
				break;
			case SPECIAL:
				if (!instruction.srcIsReference()) {
					aCode = instruction.src.evaluate(nextWords);
				} else {
					// a reference
					// find the reference's label, get instruction that's at
					// if that instruction has an address, we can use it (back reference not over another label)
					// if the instruction's address is short, we can use a short value here
					ReferenceOpNode refOp = (ReferenceOpNode) instruction.src;
					String labelName = refOp.labelName;
					Label label = labelTable.labels.get(labelName.toUpperCase());
					int instructionIndex = label.instructionIndex;
					Instruction labelInstruction = instructions.get(instructionIndex);
					if (labelInstruction.address != -1) {
						refOp.resolve(labelInstruction.address);
						aCode = instruction.src.evaluate(nextWords);
					} else {
					    // we don't know the address of the label, it hasn't been resolved yet
						break;
					}
				}
				code = Dcpu.gencmd_nbi(Dcpu.SpecialOp.byName(instruction.name).code, aCode);
				instruction.setBin(code, nextWords);
				setAddress(instruction, previous);
				break;

			default:
				break;
			}
			previous = instruction;
		}
		
		System.out.println("INSTRUCTIONS:");
		System.out.println(instructions);

		/*
		DOTTreeGenerator gen2 = new DOTTreeGenerator();  
	    StringTemplate st = gen2.toDOT(tree);  
	    System.out.println(st);
	    */
	}

	private static void setAddress(Instruction instruction, Instruction previous) {
		int lastAddress = -1;
		int lastLength = 0;
		if (previous != null) {
			lastAddress = previous.address;
			lastLength = previous.getLength();
		} else {
			lastAddress = 0;
			lastLength = 0;
		}
		if (lastAddress != -1) {
			instruction.address = lastAddress + lastLength;
		}
	}
}
