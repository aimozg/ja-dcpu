package dcpu;

import dcpu.Dcpu.BasicOp;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AssemblerTest {

    private Assembler assembler;

    private static String[] REGISTERS = new String[]{"A", "B", "C", "X", "Y", "Z", "I", "J"};
    private static String[] VAL_COMMANDS = new String[]{"PUSHPOP", "PEEK", "PICK", "SP", "PC", "EX"};
    private static Map<String, Integer> AVALUES = new LinkedHashMap<String, Integer>();
    private static Map<String, Integer> BVALUES = new LinkedHashMap<String, Integer>();
    private static final String B_BIG_LITERAL = "f154"; // used where a word is needed for B, e.g. "SET [0xf154 + I], 0x20"
    private static final String A_BIG_LITERAL = "face"; // used where a word is needed for A, e.g. "SET X, [0xface]
    private static Integer[] EXTRA_WORDS = new Integer[]{
            0, 0, 0, 0, 0, 0, 0, 0, // Register
            0, 0, 0, 0, 0, 0, 0, 0, // [Register]
            1, 1, 1, 1, 1, 1, 1, 1, // [Register + NW]
            0, 0, 1, 0, 0, 0, 1, 1, // PUSHPOP, PEEK, PICK x, SP, PC, EX, [NW], NW
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0};

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
        for (int i = 0; i < 0x20; i++) { // TODO: FIX 0 case (== -1 constant)
            AVALUES.put(String.format("%s", Integer.toString(i - 1)), 0x20 + i); // -1 .. 30
            // if (i > 0) BVALUES.put(String.format("%s", Integer.toString(i - 1)), 0x1F); // 0 .. 30 in B should be treated as NW
        }
    }

    @Before
    public void setUp() {
        assembler = new Assembler();
    }

    @Test
    public void testAssemblePureComments() {
        char[] code = assembler.assemble("; this is a comment");
        assertEquals("Should be no code", 0, code.length);
    }

    @Test
    public void testNegativeLiterals() throws Exception {
        // Test assembling [REG-LITERAL] to same opcode as [REG+0x10000-LITERAL]
        char[] bin = assembler.assemble(
                "SET A,[A-0x10]\n" +
                        "SET A,[A+0xfff0]\n"
        );
        char[] expected = new char[]{0x4001, (char) 0xfff0, 0x4001, (char) 0xfff0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        assertEquals(4, bin.length);
        assertEquals(bin[0], bin[2]);
        assertEquals(bin[1], bin[3]);
        // Test plain negative literals
        bin = assembler.assemble(
                "SET A,-2\n" +
                        "SET A,[-2]\n"
        );
        expected = new char[]{0x7c01, (char) 0xfffe, 0x7801, (char) 0xfffe};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        assertEquals(4, bin.length);
        assertEquals((char) -2, bin[1]);
        assertEquals((char) -2, bin[3]);
    }

    @Test
    public void testPlusShortable() throws Exception {
        // Test assembling [REG+LITERAL] and [LITERAL+REG] where REG<32
        char[] bin = assembler.assemble(
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
        char[] bin1 = assembler.assemble(
                "SET [0x1234 + J], A\n" +
                        "SET B, B\n"
        );
        char[] bin2 = assembler.assemble(
                "SET [J + 0x1234], A\n" +
                        "SET B, B\n"
        );
        char[] expected = new char[]{0x02e1, 0x1234, 0x0421};
        assertArrayEquals(expected, bin1);
        assertArrayEquals(expected, bin2);
    }

    @Ignore("Needs support for parsing maths")
    @Test
    public void testMathsInOps() throws Exception {
        char[] bin2 = assembler.assemble(
                "SET A, 0x8041\n" +
                        "DAT 0\n"
        );
        char[] bin1 = assembler.assemble(
                "SET A, 0x8000 + 32 * 2 + 1\n" +
                        "DAT 0\n"
        );
        assertArrayEquals(bin2, bin1);
    }

    @Test
    public void testReserve() throws Exception {
        char[] bin = assembler.assemble(
                "        SET A, 0\n" +
                        "        SET PC, jump\n" +
                        ":area   reserve 2 dat 0x00aa\n" +
                        ":jump   SET A, PC"
        );
        char[] expected = new char[]{
                0x8401,
                0x7f81, 0x0005,
                0x00aa, 0x00aa,
                0x7001
        };
        assertArrayEquals("bin: " + TestUtils.displayExpected(expected, bin), expected, bin);
    }

    @Test
    public void testLabelsAreCaseInsensitive() throws Exception {
        char[] bin = assembler.assemble(
                "           set pc, starthere\n" +
                        ":startHere set a, 1\n" +
                        "           set pc, endOfFile\n" +
                        "           set a, 0\n" +
                        ":endoffile\n" +
                        "           hlt\n"
        );
        char[] expected = new char[]{
                0x7f81, 0x0002,
                0x8801,
                0x7f81, 0x0006,
                0x8401,
                0x0000
        };
        assertArrayEquals("bin: " + TestUtils.displayExpected(expected, bin), expected, bin);
    }

    @Test
    public void testLabelsWithFullStopsSupported() throws Exception {
        char[] bin = assembler.assemble(
                "          set pc, .LBB0_.1\n" +
                        ":.LBB0_.1 set a, 1\n" +
                        "          hlt\n"
        );
        char[] expected = new char[]{
                0x7f81, 0x0002,
                0x8801,
                0x0000
        };
        assertArrayEquals("bin: " + TestUtils.displayExpected(expected, bin), expected, bin);
    }

    @Test
    public void testOpcodes() throws Exception {
        // Test every OP command except NBI for every A,B combination.
        // We also test every upper and lower case variation of both op command and codes and literals
        // "PICK x" is special cased and ignored here
        for (BasicOp op : BasicOp.values()) {
            int opCode = op.code;
            for (Entry<String, Integer> firstArg : BVALUES.entrySet()) {
                String firstKey = firstArg.getKey(); // e.g. "A"
                if (firstKey.equals("PICK")) continue;
                Integer bbbbb = firstArg.getValue(); // e.g. 0
                int firstExtraWords = EXTRA_WORDS[bbbbb];

                for (Entry<String, Integer> secondArg : AVALUES.entrySet()) {
                    String secondKey = secondArg.getKey();
                    if (secondKey.equals("PICK")) continue;
                    Integer aaaaaa = secondArg.getValue();
                    int secondExtraWords = EXTRA_WORDS[aaaaaa];
                    // OP B, A = (6 bits for A) (5 bits for B) (5 bits for opcode)
                    int actualOpcode = opCode;
                    char expectedCode = (char) (((0x001f & actualOpcode) + (0x03e0 & (bbbbb << 5)) + (0xfc00 & (aaaaaa << 10))) & 0xffff);
                    if (firstKey.equals("PUSHPOP")) firstKey = "PUSH";
                    if (secondKey.equals("PUSHPOP")) secondKey = "POP";
                    String assemblyLower = op.name.toLowerCase() + " " + firstKey + ", " + secondKey;
                    // System.out.println(assemblyLower);
                    assertAssembly(assemblyLower, expectedCode, secondExtraWords, firstExtraWords);
                    String assemblyUpper = op.name.toUpperCase() + " " + firstKey + ", " + secondKey;
                    assertAssembly(assemblyUpper, expectedCode, secondExtraWords, firstExtraWords);
                }
            }
        }
    }

    private void assertAssembly(String assembly, char expectedCode, int aExtraWords, int bExtraWords) {
        // System.out.println("assembly: " + assembly + ", aExtra = " + aExtraWords + ", bExtra = " + bExtraWords);
        int numWords = 1 + aExtraWords + bExtraWords;
        char[] code = assembler.assemble(assembly);
        char[] expected = new char[numWords];
        expected[0] = expectedCode;

        String firstLiteralToUse = aExtraWords > 0 ? A_BIG_LITERAL : B_BIG_LITERAL; // decide if the first extra word is on A or B when we only have 1 extra word
        if (numWords > 1) expected[1] = (char) (Integer.parseInt(firstLiteralToUse, 16) & 0xffff);
        if (numWords > 2)
            expected[2] = (char) (Integer.parseInt(B_BIG_LITERAL, 16) & 0xffff); // will always be B if we get 3 words
        assertArrayEquals("Testing '" + assembly + "', " + TestUtils.displayExpected(expected, code), expected, code);
    }

}
