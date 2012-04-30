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
    private Integer lineno;

    /**
     * Reference to label
     */
    private class Reference {
        final String name;
        final char position;
        final int lineat;

        Reference(String name, char position, int lineat) {
            this.name = name;
            this.position = position;
            this.lineat = lineat;
        }
    }

    private List<Reference> references = new LinkedList<Reference>();
    private Map<String, Character> symbols = new HashMap<String, Character>();
    private char[] buffer = new char[256];
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
        buffer = new char[256];
        counter = 0;
    }

    /**
     * Generate AsmMap
     */
    public boolean genMap = false;

    public AsmMap asmmap;

    // buffer to hold NextWord values before they are written out
    private List<AppendableWord> newWords = new ArrayList<AppendableWord>();

    private class AppendableWord {
        public char value;
        public boolean code;

        public AppendableWord(char value, boolean code) {
            this.value = value;
            this.code = code;
        }
    }

    public char[] assemble(String s) {
        return assemble(new StringReader(s));
    }

    public char[] assemble(Reader r) {
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
                    // TODO hlt is deprecated. Use HCF instead
                    append((char) 0, false);
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
            Character value = symbols.get(reference.name.toLowerCase());
            if (value == null)
                fail("Unresolved reference to " + reference.name.toLowerCase() + " at [" + reference.lineat + "]");
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
                    append(c, false);
                }
            } else if (accept(numPattern)) {
                append((char) tokenToInt(), false);
            } else if (accept(idPattern)) {
                append((char) 0, false);
                references.add(new Reference(token, (char) (counter - 1), stokizer.lineno()));
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
                    char value = (char) tokenToInt();
                    for (int i = 0; i < numBytes; i++) {
                        append(value, false);
                    }
                }
            }
        }
    }

    private void oper() throws IOException {
        newWords.clear(); // have to push A's new word *before* B's new word, but we read B first, so save them up to end
        require(idPattern, "operation");
        BasicOp bop = BasicOp.byName(token.toUpperCase());
        SpecialOp sop = null;
        if (bop == null) {
            sop = SpecialOp.byName(token.toUpperCase());
            if (sop == null) {
                fail("Bad operation " + token);
            }
        }
        int op_pc = counter;
        append((char) 0, true);
        // TODO allow no-args HCF and RFI
        Param pb = param(sop != null); // this is an A param if it's a SOP
        int b = pb.acode();
        if (b == -1) fail("Bad operand b");
        if (bop != null) {
            require(",", "comma");
            Param pa = param(true);
            int a = pa.acode();
            if (a == -1) fail("Bad operand a");
            buffer[op_pc] = gencmd(bop.code, b, a);
        } else {
            buffer[op_pc] = gencmd_nbi(sop.code, b);
        }
        // add the saved newWords
        for (AppendableWord a : newWords) {
            append(a.value, a.code);
        }
    }

    private Param param(boolean isA) throws IOException {
        SimpleParam p = simpleParam(true, isA);
        if (p != null) return p;
        require("[", "parameter");
        p = simpleParam(false, isA);
        if (p == null) fail("Bad parameter1");
        if (accept("+") || accept("-")) {
            // maybe bad solution for handling "-literal", but no better idea yet
            boolean neg = token.equals("-");
            SimpleParam p2 = simpleParam(false, isA);
            if (p2 == null) fail("Bad parameter2");
            if (neg && !(p2 instanceof SimpleConstParam)) fail("Bad parameter2");
            if (neg) {
                newWords.get(0).value = (char) (0x10000 - newWords.get(0).value);
            }
            require("]", "parameter");
            return new ParamSum(p, p2);
        }
        require("]", "parameter");
        return new ParamMem(p);
    }

    private SimpleParam simpleParam(boolean canBeShort, boolean isA) throws IOException {
        if (accept(idPattern)) {
            Reg reg = Reg.byName(token.toUpperCase());
            if (reg != null) {
                if (reg == Reg.PICK) {
                    int sgn = accept("-") ? -1 : 1;
                    if (accept(numPattern)) {
                        int pickValue = tokenToInt() * sgn;
                        newWords.add(0, new AppendableWord((char) pickValue, true));
                    } else {
                        return null;
                    }
                }
                return new SimpleRegisterParam(reg);
            }
            append((char) 0, true);
            Reference ref = new Reference(token, (char) (counter - 1), stokizer.lineno());
            references.add(ref);
            return new SimpleSymbolParam(token);
        } else {
            int sgn = accept("-") ? -1 : 1;
            if (accept(numPattern)) {
                int val = sgn * tokenToInt();
                if (val == 65535) val = -1; // special case 0xFFFF to be -1 so it fits in a short literal
                if (!isA && val < 0) return null;
                // a const in B should always append the val
                if (!isA || !canBeShort || val >= 31 || val < -1) {
                    // append((short) val, true);
                    newWords.add(0, new AppendableWord((char) val, true)); // add at start of list as we need them in reverse order
                }
                return new SimpleConstParam((short) val, isA);
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
        symbols.put(token.toLowerCase(), (char) counter);
        if (genMap) asmmap.symbolMap.put(token.toLowerCase(), (char) counter);
    }

    private void macro() {
        throw new UnsupportedOperationException(); // TODO write Assembler.macro method body
    }

    private void append(char s, boolean code) {
        if (counter == buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length * 3 / 2);
        }
        if (genMap) {
            asmmap.binMap.put((short) counter, lineno);
            if (code) asmmap.code.set(counter);
            if (!asmmap.srcMap.containsKey(lineno)) {
                asmmap.srcMap.put(lineno, (char) counter);
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
        final boolean isA;

        public SimpleConstParam(short value, boolean isA) {
            this.value = value;
            this.isA = isA;
        }

        @Override
        int acode() {
            // int i = value & 0xffff; // why did value used to be anded with 0xffff?
            if (isA && (value >= -1 && value <= 30)) return A_CONST + value + 1; // adjust for -1 ... 30
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
