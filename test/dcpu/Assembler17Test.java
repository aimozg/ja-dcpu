package dcpu;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Assembler17Test {
    private Assembler assembler;

    @Before
    public void setUp() {
        assembler = new Assembler();
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
    public void testSetSmallMemoryAddressUsesExtraWord() {
        char[] bin = assembler.assemble(
                "SET 0, A\n"
        );
        char[] exp = new char[]{0x03e1, 0};
        assertArrayEquals(TestUtils.displayExpected(exp, bin), exp, bin);
    }

    @Test
    public void testSetNegativeBValueFails() {
        boolean threwIAE = false;
        try {
            assembler.assemble("SET -1, A\n");
        } catch (IllegalArgumentException iae) {
            threwIAE = true;
        }
        assertEquals("throws exception", true, threwIAE);
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

}
