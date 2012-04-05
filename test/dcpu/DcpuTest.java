package dcpu;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DcpuTest {

	@Test
	public void testInitialDcpuValues() {
		Dcpu dcpu = new Dcpu();
		
		// check memory
		for (int i = 0; i < 0x10000; i++) {
			assertEquals(0, dcpu.memget(i));
		}

		// check register values
		// only value set is the M_SP which is initialised to 0xffff
		short[] registerStartingValues = new short[] {0, 0, 0, 0, 0, 0, 0, 0, 0, (short) 0xffff, 0, 0, 0, 0};
		for (int i = 0; i < registerStartingValues.length; i++) {
			assertEquals(registerStartingValues[i], dcpu.memget(Dcpu.M_A + i));
		}
		
		// check constants
		for (int i = Dcpu.M_CV; i < Dcpu.M_CV + 32; i++) {
			assertEquals(i - Dcpu.M_CV, dcpu.memget(i));
		}
	}
	

}
