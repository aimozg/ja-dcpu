package test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
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
import dcpu.InstructionGenerator;
import dcpu.antlr.Instruction;
import dcpu.antlr.Label;
import dcpu.antlr.node.ReferenceOpNode;

public class TestAssemblerGrammar {

	public static void main(String[] args) throws RecognitionException, IOException {
		CharStream charStream = new ANTLRReaderStream(new InputStreamReader(TestAssemblerGrammar.class.getResourceAsStream("Test.asm")));
		DCPULexer lexer = new DCPULexer(charStream);
		
		TokenStream tokenStream = new CommonTokenStream(lexer);
		DCPUParser parser = new DCPUParser(tokenStream);
		program_return program = parser.program();
		
		CommonTree tree = (CommonTree) program.getTree();
        System.out.println("tree:\n" + tree.toStringTree());

        CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tree);
        Map<String, Label> labels = parser.labels;
		
        InstructionGenerator gen = new InstructionGenerator(nodeStream, labels);
		AntlrAssembler.AssemblerContext context = new AssemblerContext();
		context.useShortLiterals = true;
		
		// running program() will fill in all the labels
		gen.program();
		List<Instruction> instructions = gen.instructions;
		
		
		System.out.println("LABELS:");
		System.out.println(labels.values());
		System.out.println("INSTRUCTIONS:");
		System.out.println(instructions);
		System.out.println(instructions.size());

		// check for any illegal labels (only trailing references count
		for (Entry<String, Label> entry : labels.entrySet()) {
            Label label = entry.getValue();
            if (label.instructionIndex >= instructions.size()) {
                System.err.printf("ERROR: label %s refers to unknown instruction\n", label.name);
            }
        }
		
		List<ReferenceOpNode> references = gen.references;
		
		
		/*
		DOTTreeGenerator gen2 = new DOTTreeGenerator();  
	    StringTemplate st = gen2.toDOT(tree);  
	    System.out.println(st);
	    */
	}
}
