package test;

import java.io.IOException;
import java.io.InputStreamReader;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import dcpu.AntlrAssembler;
import dcpu.AntlrAssembler.AssemblerContext;
import dcpu.ByteGenerator;
import dcpu.DCPULexer;
import dcpu.DCPUParser;
import dcpu.DCPUParser.program_return;
import dcpu.antlr.ANTLRNoCaseReaderStream;

public class TestAssemblerGrammar {

	public static void main(String[] args) throws RecognitionException, IOException {
		CharStream charStream = new ANTLRNoCaseReaderStream(new InputStreamReader(TestAssemblerGrammar.class.getResourceAsStream("Test.asm")));
		DCPULexer lexer = new DCPULexer(charStream);
		
		TokenStream tokenStream = new CommonTokenStream(lexer);
		DCPUParser parser = new DCPUParser(tokenStream);
		program_return program = parser.program();
		
		CommonTree tree = (CommonTree) program.getTree();
        System.out.println("tree:\n" + tree.toStringTree());
		CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tree);
		ByteGenerator gen = new ByteGenerator(nodeStream);
		AntlrAssembler.AssemblerContext context = new AssemblerContext();
		char[] binary = gen.program(context);
		for (int i = 0; i < binary.length; i++) {
			System.out.printf("0x%04x\n", (int) binary[i]);
		}
	}
}
