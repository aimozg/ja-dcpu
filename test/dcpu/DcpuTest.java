package dcpu;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DcpuTest {

    private Assembler assembler;
    private Dcpu dcpu;

    @Before
    public void setUp() {
        assembler = new Assembler();
        dcpu = new Dcpu();
    }

    @Test
    public void testInitialDcpuValues() {
        // check memory
        for (int i = 0; i < 0x10000; i++) {
            assertEquals(0, dcpu.memget(i));
        }

        // check register values
        // only value set is the M_SP which is initialised to 0xffff
        short[] registerStartingValues = new short[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < registerStartingValues.length; i++) {
            assertEquals(registerStartingValues[i], dcpu.memget(Dcpu.M_A + i));
        }

        // check constants
        for (int i = Dcpu.M_CV; i < Dcpu.M_CV + 32; i++) {
            assertEquals(i - Dcpu.M_CV, dcpu.memget(i));
        }
    }

    @Test
    public void testAdd() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET O, 1\n" +
                "SET X, 1\n" +
                "SET Y, 2\n" +
                "ADD X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 3, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 2, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", 0, dcpu.getreg(Dcpu.Reg.O));

        dcpu.reset();
        dcpu.upload(assembler.assemble(
                "SET O, 0\n" +
                "SET X, 0xfff0\n" +
                "SET Y, 0x0011\n" +
                "ADD X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0x11, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", 1, dcpu.getreg(Dcpu.Reg.O));
        
    }
    
    public void testSub() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET O, 1\n" +
                "SET X, 1\n" +
                "SET Y, 2\n" +
                "SUB Y, X\n"
        ));
        dcpu.run(4);
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", 0, dcpu.getreg(Dcpu.Reg.O));

        dcpu.reset();
        dcpu.upload(assembler.assemble(
                "SET O, 0\n" +
                "SET X, 0\n" +
                "SET Y, 1\n" +
                "SUB X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", (short) 0xffff, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", (short) 0xffff, dcpu.getreg(Dcpu.Reg.O));
    }
    
    @Test
    public void testMul() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET O, 1\n" +
                "SET X, 3\n" +
                "SET Y, 4\n" +
                "MUL X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 12, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 4, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", 0, dcpu.getreg(Dcpu.Reg.O));

        dcpu.reset();
        dcpu.upload(assembler.assemble(
                "SET O, 0\n" +
                "SET X, 0xffff\n" +
                "SET Y, 0x10\n" +
                "MUL X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", (short) 0xfff0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0x10, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", (short) 0x000f, dcpu.getreg(Dcpu.Reg.O));
    }

    @Test
    public void testDiv() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET O, 1\n" +
                "SET X, 12\n" +
                "SET Y, 4\n" +
                "DIV X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 3, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 4, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", 0, dcpu.getreg(Dcpu.Reg.O));

        dcpu.reset();
        dcpu.upload(assembler.assemble(
                "SET O, 0\n" +
                "SET X, 0x00ff\n" +
                "SET Y, 0x1234\n" +
                "DIV X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", (short) 0x0000, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0x1234, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", (short) 0x0e02, dcpu.getreg(Dcpu.Reg.O));
    }


}
