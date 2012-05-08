package dcpu;

import java.io.Reader;
import java.io.StringReader;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import dcpu.DCPUParser.program_return;
import dcpu.antlr.ANTLRNoCaseReaderStream;

public class AntlrAssembler {
    private AssemblerContext context;
    
    public AntlrAssembler() {
        context = new AssemblerContext();
    }
    
    public static class AssemblerContext {
        public boolean genMap = false;
        public AsmMap asmmap = new AsmMap();
    }

    public void reset() {
        context.asmmap = new AsmMap();
    }

    public char[] assemble(String s) {
        return assemble(new StringReader(s));
    }

    public char[] assemble(Reader r) {
        reset();
        
        char[] bin = new char[] {};
        try {
            CharStream charStream = new ANTLRNoCaseReaderStream(r);
            DCPULexer lexer = new DCPULexer(charStream);
            
            TokenStream tokenStream = new CommonTokenStream(lexer);
            DCPUParser parser = new DCPUParser(tokenStream);
            program_return program = parser.program();
            CommonTree tree = (CommonTree) program.getTree();
            if (tree.getChildCount() == 0) {
                System.err.println("Error: Input did not generate any code.");
                return bin;
            }
            // System.out.println("tree:\n" + tree.toStringTree());

            CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tree);
            ByteGenerator gen = new ByteGenerator(nodeStream);
            bin = gen.program(context);
        } catch (Exception e) {
            System.err.println("Failed to assemble given input: " + e.getMessage());
        }
        return bin;
    }

    public void setGenerateMap(boolean generate) {
        context.genMap = generate;
    }

    public AsmMap getAsmMap() {
        return context.asmmap;
    }    

}
