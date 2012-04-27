package dcpu;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class Assembler14Test {
    private Assembler assembler;

    @Before
    public void setUp() {
        assembler = new Assembler();
    }

    @Test
    public void testSetEXToMinusOne() {
        short[] bin = assembler.assemble("SET EX, -1\n");
        short[] exp = new short[]{(short) 0x83A1};
        TestUtils.printShorts(bin);
        assertArrayEquals("binary", exp, bin);
    }

    @Test
    public void testSetToFFFFIsMinusOne() {
        short[] bin = assembler.assemble("SET X, 0xFFFF\n");
        short[] exp = new short[]{(short) 0x8061};
        TestUtils.printShorts(bin);
        assertArrayEquals("binary", exp, bin);
    }

    @Test
    public void testSetAtoPOP() {
        short[] bin = assembler.assemble(
                "SET A, POP\n"
        );
        short[] exp = new short[]{(short) 0x6001};
        TestUtils.printShorts(bin);
        assertArrayEquals("binary", exp, bin);
    }

    @Test
    public void testSetSmallMemoryAddressUsesExtraWord() {
        short[] bin = assembler.assemble(
                "SET 0, A\n"
        );
        short[] exp = new short[]{(short) 0x03e1, 0};
        TestUtils.printShorts(bin);
        assertArrayEquals("binary", exp, bin);
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
}
