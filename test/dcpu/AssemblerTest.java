package dcpu;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssemblerTest {

	@Test
	public void testAssemblePureComments() {
		Assembler assembler = new Assembler();
		short[] code = assembler.assemble("; this is a comment");
		assertEquals("Should be no code", 0, code.length);
	}
	
	@Test
	public void testSimpleInstructions() throws Exception {
		Assembler assembler = new Assembler();
		short[] code = assembler.assemble("SET A, 0x30");
		assertArrayEquals(new short[] {0x7c01, 0x0030}, code);
	}

    private static void printBytecode(short[] bytecode) {
        for (short i : bytecode) {
            System.out.printf("%04x ", i & 0xffff);
        }
        System.out.println();
    }
}
