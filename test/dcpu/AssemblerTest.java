package dcpu;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AssemblerTest {

    private Assembler assembler;

    private static String[] REGISTERS = new String[]{"A", "B", "C", "X", "Y", "Z", "I", "J"};
    private static String[] VAL_COMMANDS = new String[]{"POP", "PEEK", "PUSH", "SP", "PC", "O"};
    private static Map<String, Integer> AVALUES = new LinkedHashMap<String, Integer>();
    private static Map<String, Integer> BVALUES = new LinkedHashMap<String, Integer>();
    private static final String A_BIG_LITERAL = "f154"; // used where a word is needed for A, e.g. "SET [0xf154 + I], 0x20"
    private static final String B_BIG_LITERAL = "face"; // used where a word is needed for B, e.g. "SET A, [0xface]
    private static Integer[] EXTRA_WORDS = new Integer[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static String[] OP_COMMANDS = new String[]{"NBI", "SET", "ADD", "SUB", "MUL", "DIV", "MOD", "SHL", "SHR", "AND", "BOR", "XOR", "IFE", "IFN", "IFG", "IFB"};

    static {
        for (int i = 0; i < REGISTERS.length; i++) {
            AVALUES.put(REGISTERS[i].toUpperCase(), 0 + i);
            BVALUES.put(REGISTERS[i].toUpperCase(), 0 + i);
        }
        for (int i = 0; i < REGISTERS.length; i++) {
            AVALUES.put("[" + REGISTERS[i].toUpperCase() + "]", 8 + i);
            BVALUES.put("[" + REGISTERS[i].toUpperCase() + "]", 8 + i);
        }
        for (int i = 0; i < REGISTERS.length; i++) {
            AVALUES.put("[0x" + A_BIG_LITERAL.toUpperCase() + " + " + REGISTERS[i].toUpperCase() + "]", 16 + i);
            BVALUES.put("[0x" + B_BIG_LITERAL.toUpperCase() + " + " + REGISTERS[i].toUpperCase() + "]", 16 + i);
            AVALUES.put("[" + REGISTERS[i].toUpperCase() + " + 0x" + A_BIG_LITERAL.toUpperCase() + " ]", 16 + i);
            BVALUES.put("[" + REGISTERS[i].toUpperCase() + " + 0x" + B_BIG_LITERAL.toUpperCase() + " ]", 16 + i);
        }
        for (int i = 0; i < VAL_COMMANDS.length; i++) {
            AVALUES.put(VAL_COMMANDS[i].toUpperCase(), 0x18 + i);
            BVALUES.put(VAL_COMMANDS[i].toUpperCase(), 0x18 + i);
        }
        AVALUES.put("[0x" + A_BIG_LITERAL.toUpperCase() + "]", 0x1e);
        AVALUES.put("0x" + A_BIG_LITERAL.toUpperCase(), 0x1f);
        BVALUES.put("[0x" + B_BIG_LITERAL.toUpperCase() + "]", 0x1e);
        BVALUES.put("0x" + B_BIG_LITERAL.toUpperCase(), 0x1f);
        for (int i = 0; i < 0x20; i++) {
            AVALUES.put(String.format("0x%02X", i), 0x20 + i);
            BVALUES.put(String.format("0x%02X", i), 0x20 + i);
        }

        // do it all again in lower case
        for (int i = 0; i < REGISTERS.length; i++) {
            AVALUES.put(REGISTERS[i].toLowerCase(), 0 + i);
            BVALUES.put(REGISTERS[i].toLowerCase(), 0 + i);
        }
        for (int i = 0; i < REGISTERS.length; i++) {
            AVALUES.put("[" + REGISTERS[i].toLowerCase() + "]", 8 + i);
            BVALUES.put("[" + REGISTERS[i].toLowerCase() + "]", 8 + i);
        }
        for (int i = 0; i < REGISTERS.length; i++) {
            AVALUES.put("[0x" + A_BIG_LITERAL.toLowerCase() + " + " + REGISTERS[i].toLowerCase() + "]", 16 + i);
            BVALUES.put("[0x" + B_BIG_LITERAL.toLowerCase() + " + " + REGISTERS[i].toLowerCase() + "]", 16 + i);
            AVALUES.put("[" + REGISTERS[i].toLowerCase() + " + 0x" + A_BIG_LITERAL.toLowerCase() + " ]", 16 + i);
            BVALUES.put("[" + REGISTERS[i].toLowerCase() + " + 0x" + B_BIG_LITERAL.toLowerCase() + " ]", 16 + i);
        }
        for (int i = 0; i < VAL_COMMANDS.length; i++) {
            AVALUES.put(VAL_COMMANDS[i].toLowerCase(), 0x18 + i);
            BVALUES.put(VAL_COMMANDS[i].toLowerCase(), 0x18 + i);
        }
        AVALUES.put("[0x" + A_BIG_LITERAL.toLowerCase() + "]", 0x1e);
        AVALUES.put("0x" + A_BIG_LITERAL.toLowerCase(), 0x1f);
        BVALUES.put("[0x" + B_BIG_LITERAL.toLowerCase() + "]", 0x1e);
        BVALUES.put("0x" + B_BIG_LITERAL.toLowerCase(), 0x1f);
        for (int i = 0; i < 0x20; i++) {
            AVALUES.put(String.format("0x%02x", i).toLowerCase(), 0x20 + i);
            BVALUES.put(String.format("0x%02x", i).toLowerCase(), 0x20 + i);
        }
    }

    @Before
    public void setUp() {
        assembler = new Assembler();
    }

    @Test
    public void testAssemblePureComments() {
        short[] code = assembler.assemble("; this is a comment");
        assertEquals("Should be no code", 0, code.length);
    }

    @Test
    public void testNegativeLiterals() throws Exception {
        // Test assembling [REG-LITERAL] to same opcode as [REG+0x10000-LITERAL]
        short[] bin = assembler.assemble(
                "SET A,[A-0x10]\n" +
                        "SET A,[A+0xfff0]\n"
        );
        assertEquals(4, bin.length);
        assertEquals(bin[0], bin[2]);
        assertEquals(bin[1], bin[3]);
        // Test plain negative literals
        bin = assembler.assemble(
                "SET A,-1\n" +
                        "SET A,[-1]\n"
        );
        assertEquals(4, bin.length);
        assertEquals(-1, bin[1]);
        assertEquals(-1, bin[3]);
    }

    @Test
    public void testPlusShortable() throws Exception {
        // Test assembling [REG+LITERAL] and [LITERAL+REG] where REG<32
        short[] bin = assembler.assemble(
                "SET A,[B+1]\n" +
                        "SET B,[2+C]\n" +
                        "DAT 0\n"
        );
        assertEquals(5, bin.length);
        assertEquals(Dcpu.gencmd(Dcpu.O_SET, Dcpu.A_A, Dcpu.A_M_NW_B), bin[0]);
        assertEquals(bin[1], 1);
        assertEquals(Dcpu.gencmd(Dcpu.O_SET, Dcpu.A_B, Dcpu.A_M_NW_C), bin[2]);
        assertEquals(bin[3], 2);
        assertEquals(bin[4], 0);
    }

    @Test
    public void testRegisterPlusOffsetIsCommutativeAndDoesntSwallowNextCommand() throws Exception {
        short[] bin1 = assembler.assemble(
                "SET [0x1234 + J], A\n" +
                "SET B, B\n"
        );
        short[] bin2 = assembler.assemble(
                "SET [J + 0x1234], A\n" +
                "SET B, B\n"
        );
        short[] expected = new short[] {0x0171, 0x1234, 0x0411};
        assertArrayEquals(expected, bin1);
        assertArrayEquals(expected, bin2);
    }

    @Ignore("Needs support for parsing maths")
    @Test
    public void testMathsInOps() throws Exception {
        short[] bin2 = assembler.assemble(
                "SET A, 0x8041\n" +
                "DAT 0\n"
        );
        short[] bin1 = assembler.assemble(
                "SET A, 0x8000 + 32 * 2 + 1\n" +
                "DAT 0\n"
        );
        assertArrayEquals(bin2, bin1);
    }
    
    @Test
    public void testReserve() throws Exception {
        short[] bin = assembler.assemble(
                "        SET A, 0\n" +
                "        SET PC, jump\n" +
                ":area   reserve 2 dat 0x00aa" +
                ":jump   SET A, PC"
        );
        short[] expected = new short[] {
                (short) 0x8001, 
                        0x7dc1, 0x0005, 
                        0x00aa, 0x00aa, 
                        0x7001
        };
        assertArrayEquals("bin", expected, bin);
    }

    @Test
    public void testOpcodes() throws Exception {
        // Test every OP command except NBI for every A,B combination.
        // We also test every upper and lower case variation of both op command and codes and literals
        for (int opCode = 1; opCode < OP_COMMANDS.length; opCode++) {
            for (Entry<String, Integer> aEntry : AVALUES.entrySet()) {
                String aKey = aEntry.getKey(); // e.g. "A"
                Integer aValue = aEntry.getValue(); // e.g. 0
                int aExtraWords = EXTRA_WORDS[aValue];

                for (Entry<String, Integer> bEntry : BVALUES.entrySet()) {
                    String bKey = bEntry.getKey();
                    Integer bValue = bEntry.getValue();
                    int bExtraWords = EXTRA_WORDS[bValue];
                    // OP A, B = (6 bits for B) (6 bits for A) (4 bits for opcode)
                    short expectedCode = (short) (((0x000f & opCode) + (0x03f0 & (aValue << 4)) + (0xfc00 & (bValue << 10))) & 0xffff);

                    assertAssembly(OP_COMMANDS[opCode].toLowerCase() + " " + aKey + ", " + bKey, expectedCode, aExtraWords, bExtraWords);
                    assertAssembly(OP_COMMANDS[opCode].toUpperCase() + " " + aKey + ", " + bKey, expectedCode, aExtraWords, bExtraWords);
                }
            }
        }
    }

    private void assertAssembly(String assembly, short expectedCode, int aExtraWords, int bExtraWords) {
        int numWords = 1 + aExtraWords + bExtraWords;
        short[] code = assembler.assemble(assembly);
        short[] expected = new short[numWords];
        expected[0] = expectedCode;

        String firstLiteralToUse = aExtraWords > 0 ? A_BIG_LITERAL : B_BIG_LITERAL; // decide if the first extra word is on A or B when we only have 1 extra word
        if (numWords > 1) expected[1] = (short) (Integer.parseInt(firstLiteralToUse, 16) & 0xffff);
        if (numWords > 2)
            expected[2] = (short) (Integer.parseInt(B_BIG_LITERAL, 16) & 0xffff); // will always be B if we get 3 words
        assertEquals("Expected " + numWords + " words", numWords, code.length);
        assertArrayEquals(expected, code);
    }

}
