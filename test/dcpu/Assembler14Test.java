package dcpu;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class Assembler14Test {
    private Assembler assembler;

    private void printShorts(short[] data) {
        for (short s : data) {
            System.out.printf("0x%04x ", s);
        }
        System.out.println();
    }
    
    @Before
    public void setUp() {
        assembler = new Assembler();
    }

    @Test
    public void testSetEX() {
        short[] bin = assembler.assemble(
                "SET EX, -1\n"
        );
        short[] exp = new short[]{(short) 0x83A1};
        printShorts(bin);
        assertArrayEquals("binary", exp, bin);
    }

    @Test
    public void testSetAtoPOP() {
        short[] bin = assembler.assemble(
                "SET A, POP\n"
        );
        short[] exp = new short[]{(short) 0x6001};
        printShorts(bin);
        assertArrayEquals("binary", exp, bin);
    }

    @Test
    public void testSetSmallMemory() {
        short[] bin = assembler.assemble(
                "SET 0, A\n"
        );
        short[] exp = new short[]{(short) 0x03e1, 0};
        printShorts(bin);
        assertArrayEquals("binary", exp, bin);
    }

    @Test
    public void testSetNegativeBValueFails() {
        boolean threwIAE = false;
        try {
            short[] bin = assembler.assemble(
                    "SET -1, A\n"
            );
        } catch (IllegalArgumentException iae) {
            threwIAE = true;
        }
        assertEquals("throws exception", true, threwIAE);
    }
}
