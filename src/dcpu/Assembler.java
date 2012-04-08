package dcpu;

import java.io.IOException;
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
    private static final Pattern idPattern = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
    private static final Pattern hexPattern = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern binPattern = Pattern.compile("0b\\d+");
    private static final Pattern decPattern = Pattern.compile("\\d+");
    private static final Pattern numPattern = Pattern.compile("(" + hexPattern.pattern() + ")|(" + binPattern.pattern() + ")|(" + decPattern.pattern() + ")");

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
        reset();
        stokizer = new StreamTokenizer(new StringReader(s));
        stokizer.commentChar(';');
        stokizer.quoteChar('"');
        stokizer.quoteChar('\'');
        //stokizer.wordChars('#', '#');
        stokizer.ordinaryChar('-');
        stokizer.ordinaryChars('0', '9');
        stokizer.wordChars('0', '9');
        stokizer.wordChars('_', '_');
        stokizer.slashSlashComments(false);
        stokizer.slashStarComments(false);
        stokizer.eolIsSignificant(false);
        try {
            next();
            while (!eof()) {
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
                } else if (acceptIgnoreCase("hlt")) {
                    append(gencmd_nbi(O__RESVD, 0), false);
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
            Short value = symbols.get(reference.name);
            if (value == null) fail("Unresolved reference to " + reference.name + " at [" + reference.lineat + "]");
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

    private void oper() throws IOException {
        boolean nbi = false;
        require(idPattern, "operation");
        int opcode = opcodeByName(token);
        if (opcode == -1) {
            opcode = opcodeNbiByName(token);
            if (opcode == -1) {
                fail("Bad operation " + token);
            } else {
                nbi = true;
            }
        }
        int op_pc = counter;
        append((short) 0, true);
        Param pa = param();
        int a = pa.acode();
        if (a == -1) fail("Bad operand a");
        if (!nbi) {
            require(",", "comma");
            Param pb = param();
            int b = pb.acode();
            if (b == -1) fail("Bad operand b");
            buffer[op_pc] = gencmd(opcode, a, b);
        } else {
            buffer[op_pc] = gencmd_nbi(opcode, a);
        }
    }

    private Param param() throws IOException {
        SimpleParam p = simpleParam(true);
        if (p != null) return p;
        require("[", "parameter");
        p = simpleParam(true);
        if (p == null) fail("Bad parameter1");
        if (accept("+") || accept("-")) {
            // maybe bad solution for handling "-literal", but no better idea yet
            boolean neg = token.equals("-");
            SimpleParam p2 = simpleParam(false);
            if (p2 == null) fail("Bad parameter2");
            if (neg) buffer[counter - 1] = (short) (0x10000 - buffer[counter - 1]);
            require("]", "parameter");
            return new ParamSum(p, p2);
        }
        require("]", "parameter");
        return new ParamMem(p);
    }

    private SimpleParam simpleParam(boolean canBeShort) throws IOException {
        if (accept(idPattern)) {
            int regidx = registerByName(token);
            if (regidx >= 0) return new SimpleRegisterParam(regidx);
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
        symbols.put(token, (short) counter);
        if (genMap) asmmap.symbolMap.put(token, (short) counter);
    }

    private void macro() {
        throw new UnsupportedOperationException(); // TODO write Assembler.macro method body
    }

    private void append(short s, boolean code) {
        if (counter == buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length * 3 / 2);
        }
        if (genMap) {
            int line = stokizer.lineno();
            asmmap.binMap.put((short) counter, line);
            if (code) asmmap.code.set(counter);
            if (!asmmap.srcMap.containsKey(line)) {
                asmmap.srcMap.put(line, (short) counter);
            }
        }
        buffer[counter++] = s;
    }

    private static final String[] instr_names =
            {
                    "add", "sub", "mul",
                    "div", "mod", "shl", "shr",
                    "and", "bor", "xor", "set",
                    "ifb", "ife", "ifg", "ifn"};
    private static final int[] instr_codes =
            {
                    O_ADD, O_SUB, O_MUL,
                    O_DIV, O_MOD, O_SHL, O_SHR,
                    O_AND, O_BOR, O_XOR, O_SET,
                    O_IFB, O_IFE, O_IFG, O_IFN};
    public static final String[] nbinstr_names =
            {
                    "jsr"
            };
    public static final int[] nbinstr_codes =
            {
                    O__JSR
            };
    private static final String[] reg_names =
            {"A", "B", "C", "X", "Y", "Z", "I", "J", "SP", "PC", "O", "POP", "PEEK", "PUSH"};
    private static final int[] reg_offsets =
            {0, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, -1, -1, -1};
    private static final int[] reg_selfcodes =
            {0, 1, 2, 3, 4, 5, 6, 7, 27, 28, 29, 24, 25, 26};

    private int opcodeByName(String name) {
        name = name.toLowerCase();
        for (int i = 0; i < instr_names.length; i++) {
            if (instr_names[i].equals(name)) return instr_codes[i];
        }
        return -1;
    }

    private int opcodeNbiByName(String name) {
        name = name.toLowerCase();
        for (int i = 0; i < nbinstr_names.length; i++) {
            if (nbinstr_names[i].equals(name)) return nbinstr_codes[i];
        }
        return -1;
    }

    private int registerByName(String name) {
        name = name.toUpperCase();
        for (int i = 0; i < reg_names.length; i++) {
            if (reg_names[i].equals(name)) return i;
        }
        return -1;
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
            if ((value & 0xffff) < 32) return A_CONST + value;
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
        private final int idx;

        public SimpleRegisterParam(int idx) {
            this.idx = idx;
        }

        public boolean isGP() {
            return reg_offsets[idx] >= 0;
        }

        public int offset() {
            return reg_offsets[idx];
        }

        @Override
        int acode() {
            return reg_selfcodes[idx];
        }
    }

}
