package dcpu;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

import static dcpu.Dcpu.*;

/**
 * DCPU Assembler
 */
public class Assembler {

    private StreamTokenizer stokizer;
    private String token;
    private String nexttok;
    private int itoken;
    // current line number. Cannot use StringTokenizer's one because it switches to next line too early
    // (since we are actually analyzing its previous token)
    private int lineno;

    /**
     * Reference to label
     */
    private class Reference {
        final String name;
        final short position;
        final int lineat;

        Reference(String name, short position, int lineat) {
            this.name = name;
            this.position = position;
            this.lineat = lineat;
        }
    }

    private List<Reference> references = new LinkedList<Reference>();
    private Map<String, Short> symbols = new HashMap<String, Short>();
    private short[] buffer = new short[256];
    private int counter;

    boolean eof() {
        return itoken == StreamTokenizer.TT_EOF;
    }

    boolean accept(String s) throws IOException {
        if (nexttok == null) return false;
        if (nexttok.equals(s)) {
            next();
            return true;
        }
        return false;
    }

    boolean acceptIgnoreCase(String s) throws IOException {
        if (nexttok == null) return false;
        if (nexttok.equalsIgnoreCase(s)) {
            next();
            return true;
        }
        return false;
    }

    boolean accept(Pattern p) throws IOException {
        if (nexttok == null) return false;
        if (p.matcher(nexttok).matches()) {
            next();
            return true;
        }
        return false;
    }

    void next() throws IOException {
        token = nexttok;
        itoken = stokizer.nextToken();
        nexttok = stokizer.sval;
        if (itoken == '\'' || itoken == '\"') nexttok = ((char) itoken) + nexttok + ((char) itoken);
        else if (itoken >= 0) nexttok = String.valueOf((char) itoken);
    }

    private static final Pattern strPattern = Pattern.compile("(\'[^\']*\')|(\"[^\"]*\")");
    private static final Pattern idPattern = Pattern.compile("[a-zA-Z_\\.][a-zA-Z_0-9\\.]*");
    private static final Pattern hexPattern = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern binPattern = Pattern.compile("0b\\d+");
    private static final Pattern decPattern = Pattern.compile("\\d+");
    public static final Pattern numPattern = Pattern.compile("(" + hexPattern.pattern() + ")|(" + binPattern.pattern() + ")|(" + decPattern.pattern() + ")");

    private void require(Pattern pattern, String descr) throws IOException {
        if (!accept(pattern)) {
            fail("Bad " + descr);
        }
    }

    private void require(String s, String descr) throws IOException {
        if (!accept(s)) {
            fail("Bad " + descr);
        }
    }

    private void fail(String reason) {
        throw new IllegalArgumentException("[" + stokizer.lineno() + "] " + reason);
    }


    void reset() {
        asmmap = new AsmMap();
        references.clear();
        symbols.clear();
        buffer = new short[256];
        counter = 0;
    }

    /**
     * Generate AsmMap
     */
    public boolean genMap = false;

    public AsmMap asmmap;

    public short[] assemble(String s) {
        return assemble(new StringReader(s));
    }

    public short[] assemble(Reader r) {
        reset();
        stokizer = new StreamTokenizer(r);
        stokizer.commentChar(';');
        stokizer.quoteChar('"');
        stokizer.quoteChar('\'');
        //stokizer.wordChars('#', '#');
        stokizer.ordinaryChar('-');
        stokizer.ordinaryChar('.');
        stokizer.ordinaryChars('0', '9');
        stokizer.wordChars('0', '9');
        stokizer.wordChars('_', '_');
        stokizer.wordChars('.', '.');
        stokizer.slashSlashComments(false);
        stokizer.slashStarComments(false);
        stokizer.eolIsSignificant(false);
        try {
            next();
            while (!eof()) {
                lineno = stokizer.lineno();
                if (accept("#")) {
                    if (accept("macro")) {
                        macro();
                    } else {
                        fail("Bad directive");
                    }
                } else if (accept(":")) {
                    label();
                } else if (acceptIgnoreCase("dat")) {
                    dat();
                } else if (acceptIgnoreCase("reserve")) {
                    reserve();
                } else if (acceptIgnoreCase("hlt")) {
                    append((short) 0, false);
                } else {
                    oper();
                }
            }
            /*if (accept("#macro")){

            } */
        } catch (IOException ignored) {
        }
        /*for (Map.Entry<String, Short> symbol : symbols.entrySet()) {
            System.out.printf("symbol %s = %04x\n", symbol.getKey(),symbol.getValue());
        } */
        for (Reference reference : references) {
            //System.out.printf("ref to %s @ %04x\n",reference.name,reference.position);
            Short value = symbols.get(reference.name.toLowerCase());
            if (value == null) fail("Unresolved reference to " + reference.name.toLowerCase() + " at [" + reference.lineat + "]");
            buffer[reference.position] = value;
        }
        if (buffer.length > counter) {
            buffer = Arrays.copyOf(buffer, counter);
        }
        return buffer;
    }

    private void dat() throws IOException {
        boolean cm = false;
        while (true) {
            if (cm && !accept(",")) break;
            cm = true;
            if (accept(strPattern)) {
                for (char c : token.substring(1, token.length() - 1).toCharArray()) {
                    append((short) c, false);
                }
            } else if (accept(numPattern)) {
                append((short) tokenToInt(), false);
            } else if (accept(idPattern)) {
                append((short) 0, false);
                references.add(new Reference(token, (short) (counter - 1), stokizer.lineno()));
            }
        }
    }

    private void reserve() throws IOException {
        // reserves a number of words
        // (e.g) ":input_data reserve 32 dat 0x00"
        if (accept(numPattern)) {
            int numBytes = tokenToInt();
            if (accept("dat")) {
                if (accept(numPattern)) {
                    short value = (short) tokenToInt();
                    for (int i = 0; i < numBytes; i++) {
                        append(value, false);
                    }
                }
            }
        }
    }

    private void oper() throws IOException {
        boolean nbi = false;
        require(idPattern, "operation");
        BasicOp bop = BasicOp.byName(token);
        SpecialOp sop = null;
        if (bop == null) {
            sop = SpecialOp.byName(token);
            if (sop == null) {
                fail("Bad operation " + token);
            }
        }
        int op_pc = counter;
        append((short) 0, true);
        Param pb = param();
        int b = pb.acode();
        if (b == -1) fail("Bad operand b");
        if (bop != null) {
            require(",", "comma");
            Param pa = param();
            int a = pa.acode();
            if (a == -1) fail("Bad operand a");
            buffer[op_pc] = gencmd(bop.code, a, b);
        } else {
            buffer[op_pc] = gencmd_nbi(sop.code, b);
        }
    }

    private Param param() throws IOException {
        SimpleParam p = simpleParam(true);
        if (p != null) return p;
        require("[", "parameter");
        p = simpleParam(false);
        if (p == null) fail("Bad parameter1");
        if (accept("+") || accept("-")) {
            // maybe bad solution for handling "-literal", but no better idea yet
            boolean neg = token.equals("-");
            SimpleParam p2 = simpleParam(false);
            if (p2 == null) fail("Bad parameter2");
            if (neg && !(p2 instanceof SimpleConstParam)) fail("Bad parameter2");
            if (neg) buffer[counter - 1] = (short) (0x10000 - buffer[counter - 1]);
            require("]", "parameter");
            return new ParamSum(p, p2);
        }
        require("]", "parameter");
        return new ParamMem(p);
    }

    private SimpleParam simpleParam(boolean canBeShort) throws IOException {
        if (accept(idPattern)) {
            Reg reg = Reg.byName(token);
            if (reg != null) return new SimpleRegisterParam(reg);
            append((short) 0, true);
            Reference ref = new Reference(token, (short) (counter - 1), stokizer.lineno());
            references.add(ref);
            return new SimpleSymbolParam(token);
        } else {
            int sgn = accept("-") ? -1 : 1;
            if (accept(numPattern)) {
                int val = sgn * tokenToInt();
                if (!canBeShort || val >= 32 || val < 0) append((short) val, true);
                return new SimpleConstParam((short) val);
            } else {
                return null;
            }
        }
    }

    private int tokenToInt() {
        int val;
        if (token.startsWith("0x")) val = Integer.parseInt(token.substring(2), 16);
        else if (token.startsWith("0b")) val = Integer.parseInt(token.substring(2, 2));
        else val = Integer.parseInt(token);
        return val;
    }


    private void label() throws IOException {
        require(idPattern, "label name");
        symbols.put(token.toLowerCase(), (short) counter);
        if (genMap) asmmap.symbolMap.put(token.toLowerCase(), (short) counter);
    }

    private void macro() {
        throw new UnsupportedOperationException(); // TODO write Assembler.macro method body
    }

    private void append(short s, boolean code) {
        if (counter == buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length * 3 / 2);
        }
        if (genMap) {
            asmmap.binMap.put((short) counter, lineno);
            if (code) asmmap.code.set(counter);
            if (!asmmap.srcMap.containsKey(lineno)) {
                asmmap.srcMap.put(lineno, (short) counter);
            }
        }
        buffer[counter++] = s;
    }

    private static abstract class Param {
        abstract int acode();
    }

    private static class ParamSum extends Param {
        final SimpleParam a;
        final SimpleParam b;

        public ParamSum(SimpleParam a, SimpleParam b) {
            this.a = a;
            this.b = b;
        }

        @Override
        int acode() {
            if (a instanceof SimpleRegisterParam && ((SimpleRegisterParam) a).isGP() && (b instanceof SimpleConstParam || b instanceof SimpleSymbolParam)) {
                return A_M_NW_REG + ((SimpleRegisterParam) a).offset();
            } else if (b instanceof SimpleRegisterParam && ((SimpleRegisterParam) b).isGP() && (a instanceof SimpleConstParam || a instanceof SimpleSymbolParam)) {
                return A_M_NW_REG + ((SimpleRegisterParam) b).offset();
            } else return -1;
        }
    }

    private static abstract class SimpleParam extends Param {
    }

    private static class SimpleConstParam extends SimpleParam {
        final short value;

        public SimpleConstParam(short value) {
            this.value = value;
        }

        @Override
        int acode() {
            // int i = value & 0xffff; // why did value used to be anded with 0xffff?
            if (value >=-1 && value <= 30) return A_CONST + value + 1; // adjust for -1 ... 30
            return A_NW;
        }
    }

    private static class ParamMem extends Param {

        final SimpleParam addr;

        public ParamMem(SimpleParam addr) {
            this.addr = addr;
        }

        @Override
        int acode() {
            if (addr instanceof SimpleSymbolParam || addr instanceof SimpleConstParam) {
                return A_M_NW;
            } else if (addr instanceof SimpleRegisterParam && ((SimpleRegisterParam) addr).isGP()) {
                return A_M_REG + ((SimpleRegisterParam) addr).offset();
            } else return -1;
        }
    }

    private static class SimpleSymbolParam extends SimpleParam {
        @SuppressWarnings("unused")
        final String name;

        private SimpleSymbolParam(String name) {
            this.name = name;
        }

        @Override
        int acode() {
            return A_NW;
        }
    }

    private static class SimpleRegisterParam extends SimpleParam {
        private final Reg reg;

        public SimpleRegisterParam(Reg reg) {
            this.reg = reg;
        }

        public boolean isGP() {
            return reg.offset < 8;
        }

        public int offset() {
            return reg.offset;
        }

        @Override
        int acode() {
            return reg.acode;
        }
    }

}
