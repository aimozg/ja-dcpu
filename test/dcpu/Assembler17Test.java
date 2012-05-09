package dcpu;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Before;
import org.junit.Test;

public class Assembler17Test {
    private AntlrAssembler assembler;

    @Before
    public void setUp() {
        assembler = new AntlrAssembler();
    }

    @Test
    public void testSetEXToMinusOne() {
        char[] bin = assembler.assemble("SET EX, -1\n");
        char[] exp = new char[]{0x83A1};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
    }

    @Test
    public void testSetToFFFFIsMinusOne() {
        char[] bin = assembler.assemble("SET X, 0xFFFF\n");
        char[] exp = new char[]{0x8061};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
    }

    @Test
    public void testSetAtoPOP() {
        char[] bin = assembler.assemble(
                "SET A, POP\n"
        );
        char[] exp = new char[]{0x6001};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
    }

    @Test
    public void testSetBLiteralUsesExtraWord() {
        char[] bin = assembler.assemble(
                "SET 0, A\n"
        );
        char[] exp = new char[]{0x03e1, 0};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
    }

    @Test
    public void testSetNegativeBValuesConvertToChar() {
        char[] exp, bin;
        bin = assembler.assemble("SET -2, A\n");
        exp = new char[]{0x03e1, 0xfffe};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
        
        bin = assembler.assemble("SET -1, A\n");
        exp = new char[]{0x03e1, 0xffff};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
    }

    @Test
    public void testSimpleNWValues() throws Exception {
        char[] bin = assembler.assemble("SET [A + 0x0100], [B + 0x0200]\n");
        char[] exp = new char[]{0x4601, 0x0200, 0x0100};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
    }

    @Test
    public void testPickX() throws Exception {
        char[] bin = assembler.assemble(
                "set A, PICK 1\n" +
                        "HCF 0\n"
        );
        char[] expected = new char[]{
                0x6801, 0x0001,
                0x84e0
        };
        assertArrayEquals("bin: " + TestUtils.displayExpected(expected, bin), expected, bin);
    }
    
    @Test
    public void testShortBackReference() throws Exception {
        assembler.setUseShortLiterals(true);
        char[] bin = assembler.assemble(
            "   JSR f1\n" +  // this one is forward reference so doesn't know about f1 => uses NW
            "    SET A, 0\n" +
            "    RESERVE 1\n" +
            ":f1\n" +
            "    SET B, 0\n" +
            "    JSR f1"
        );
        char[] expected = new char[]{
                0x7c20, 0x0004,
                0x8401,
                0x0000,
                0x8421,
                0x9420
        };
        assertArrayEquals("bin: " + TestUtils.displayExpected(expected, bin), expected, bin);
    }

}
