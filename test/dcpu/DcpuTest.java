package dcpu;

import org.junit.Before;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DcpuTest {

    private AntlrAssembler assembler;
    private Dcpu dcpu;

    @Before
    public void setUp() {
        assembler = new AntlrAssembler();
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
        char[] registerStartingValues = new char[Dcpu.REGS_COUNT];
        for (int i = 0; i < registerStartingValues.length; i++) {
            assertEquals(registerStartingValues[i], dcpu.memget(Dcpu.M_A + i));
        }

        // check constants
        for (int i = Dcpu.M_CV; i < Dcpu.M_CV + 32; i++) {
            assertEquals("at i=" + i, (char) (i - Dcpu.M_CV - 1), dcpu.memget(i));
        }
    }

    @Test
    public void testADD() throws Exception {
        String[] setupValues = new String[]{"0x0100", "0x0200", "0x10", "0x20", "0x30", "0x40", "0x50", "0x60", "0x70", "0x80"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0x0300, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0180, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0160, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0140, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x0270});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x00f0});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x00d0});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x00b0});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0250, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x00d0, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x00b0, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0090, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0230, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x00b0, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0090, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0070, 0x0050, 0x0070});
        testCmd("ADD", setupValues, expected);
    }

    @Test
    public void testSUB() throws Exception {
        String[] setupValues = new String[]{"0x0100", "0x0200", "0x10", "0x20", "0x30", "0x40", "0x50", "0x60", "0x70", "0x80"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0xff00, 0xffff, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0080, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x00a0, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x00c0, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0xffff, 0x0030, 0x0050, 0xfe70});
        expected.add(new Integer[]{0x0100, 0xffff, 0x0030, 0x0050, 0xfff0});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x0010});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x0030});
        expected.add(new Integer[]{0x0100, 0xffff, 0x0030, 0xfe50, 0x0070});
        expected.add(new Integer[]{0x0100, 0xffff, 0x0030, 0xffd0, 0x0070});
        expected.add(new Integer[]{0x0100, 0xffff, 0x0030, 0xfff0, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0010, 0x0070});
        expected.add(new Integer[]{0x0100, 0xffff, 0xfe30, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0xffff, 0xffb0, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0xffff, 0xffd0, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0xffff, 0xfff0, 0x0050, 0x0070});
        testCmd("SUB", setupValues, expected);
    }

    @Test
    public void testMUL() throws Exception {
        String[] setupValues = new String[]{"0x0123", "0x0456", "0x11", "0x22", "0x33", "0x44", "0x55", "0x66", "0x77", "0x88"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0xedc2, 0x0004, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x9a98, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x73f2, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x4d4c, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0002, 0x0033, 0x0055, 0x03fa});
        expected.add(new Integer[]{0x0123, 0x0000, 0x0033, 0x0055, 0x3f38});
        expected.add(new Integer[]{0x0123, 0x0000, 0x0033, 0x0055, 0x2f6a});
        expected.add(new Integer[]{0x0123, 0x0000, 0x0033, 0x0055, 0x1f9c});
        expected.add(new Integer[]{0x0123, 0x0001, 0x0033, 0x708e, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0000, 0x0033, 0x2d28, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0000, 0x0033, 0x21de, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0000, 0x0033, 0x1694, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0000, 0xdd22, 0x0055, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0000, 0x1b18, 0x0055, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0000, 0x1452, 0x0055, 0x0077});
        expected.add(new Integer[]{0x0123, 0x0000, 0x0d8c, 0x0055, 0x0077});
        testCmd("MUL", setupValues, expected);
    }

    @Test
    public void testDIV() throws Exception {
        String[] setupValues = new String[]{"0xcab5", "0xe4a3", "0x7", "0xd", "0x30", "0x40", "0x50", "0x60", "0x70", "0x80"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0x0000, 0xc455, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0195, 0x6a00, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x021c, 0xe2ab, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x032a, 0xd400, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0xcab5, 0x007d, 0x0030, 0x0050, 0x0000});
        expected.add(new Integer[]{0xcab5, 0xe000, 0x0030, 0x0050, 0x0000});
        expected.add(new Integer[]{0xcab5, 0x2aaa, 0x0030, 0x0050, 0x0001});
        expected.add(new Integer[]{0xcab5, 0xc000, 0x0030, 0x0050, 0x0001});
        expected.add(new Integer[]{0xcab5, 0x0059, 0x0030, 0x0000, 0x0070});
        expected.add(new Integer[]{0xcab5, 0xa000, 0x0030, 0x0000, 0x0070});
        expected.add(new Integer[]{0xcab5, 0xd555, 0x0030, 0x0000, 0x0070});
        expected.add(new Integer[]{0xcab5, 0x4000, 0x0030, 0x0001, 0x0070});
        expected.add(new Integer[]{0xcab5, 0x0035, 0x0000, 0x0050, 0x0070});
        expected.add(new Integer[]{0xcab5, 0x6000, 0x0000, 0x0050, 0x0070});
        expected.add(new Integer[]{0xcab5, 0x8000, 0x0000, 0x0050, 0x0070});
        expected.add(new Integer[]{0xcab5, 0xc000, 0x0000, 0x0050, 0x0070});
        testCmd("DIV", setupValues, expected);
    }

    @Test
    public void testMOD() throws Exception {
        String[] setupValues = new String[]{"0x3456", "0x0037", "0x11", "0x22", "0x33", "0x44", "0x55", "0x66", "0x77", "0x88"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0x0021, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x0046, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x0024, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x0002, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0009});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0011});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0033});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x001e, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0011, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0077});
        expected.add(new Integer[]{0x3456, 0x0000, 0x0033, 0x0055, 0x0077});
        testCmd("MOD", setupValues, expected);
    }

    @Test
    public void testSHL() throws Exception {
        String[] setupValues = new String[]{"0xa234", "0x23", "0x10", "0x20", "0x30", "0x40", "0x50", "0x60", "0x5a5a", "0x80"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0x11a0, 0x0005, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0002, 0x0030, 0x0050, 0xd2d0});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0280, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0180, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        testCmd("SHL", setupValues, expected);
    }

    @Test
    public void testSHR() throws Exception {
        String[] setupValues = new String[]{"0xa234", "0x23", "0x10", "0x20", "0x30", "0x40", "0x50", "0x60", "0x5a5a", "0x80"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0x1446, 0x8000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x4000, 0x0030, 0x0050, 0x0b4b});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x000a, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0006, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        testCmd("SHR", setupValues, expected);
    }

    @Test
    public void testAND() throws Exception {
        String[] setupValues = new String[]{"0xa3a3", "0x0505", "0x10", "0x20", "0x30", "0x40", "0x50", "0x60", "0x77", "0x80"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0x0101, 0x0000, 0x0030, 0x0050, 0x0077});
        expected.add(new Integer[]{0x0080, 0x0000, 0x0030, 0x0050, 0x0077});
        expected.add(new Integer[]{0x0020, 0x0000, 0x0030, 0x0050, 0x0077});
        expected.add(new Integer[]{0x0000, 0x0000, 0x0030, 0x0050, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0050, 0x0005});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0050, 0x0000});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0050, 0x0060});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0050, 0x0040});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0000, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0000, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0040, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0030, 0x0040, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0000, 0x0050, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0000, 0x0050, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0020, 0x0050, 0x0077});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0x0000, 0x0050, 0x0077});
        testCmd("AND", setupValues, expected);
    }

    @Test
    public void testBOR() throws Exception {
        String[] setupValues = new String[]{"0xa3a3", "0x0505", "0x51", "0x21", "0xab73", "0x42", "0x9c", "0x65", "0x3577", "0xa1"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0xa7a7, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3e7, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3e3, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x35f7});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x059d, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x00bd, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x00fd, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x00de, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xaf77, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xabf3, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab77, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3577});
        testCmd("BOR", setupValues, expected);
    }

    @Test
    public void testXOR() throws Exception {
        String[] setupValues = new String[]{"0xa3a3", "0x0505", "0x51", "0x21", "0xab73", "0x42", "0x9c", "0x65", "0x3577", "0xa1"};
        List<Integer[]> expected = new ArrayList<Integer[]>();
        expected.add(new Integer[]{0xa6a6, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa302, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3c6, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3e1, 0x0000, 0xab73, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3072});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x35d6});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3512});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x009c, 0x3535});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x0599, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x003d, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x00f9, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab73, 0x00de, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xae76, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xabd2, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab16, 0x009c, 0x3577});
        expected.add(new Integer[]{0xa3a3, 0x0000, 0xab31, 0x009c, 0x3577});
        testCmd("XOR", setupValues, expected);
    }

    private void testCmd(String cmd, String[] setupValues, List<Integer[]> expected) throws Exception {
        // run all combinations of A,B values for register/[register]/[nw + register]/[literal]
        // each run requires 10 input values, and 16 expected values for 5 values (above 4 + Overflow)
        String[] aValues = new String[]{"A", "[A]", "[0xA000 + I]", "[0xA000]"};
        String[] bValues = new String[]{"B", "[B]", "[0xB000 + J]", "[0xB000]"};
        String setup = "SET A, " + setupValues[0] + "\n" +
                "SET B, " + setupValues[1] + "\n" +
                "SET I, " + setupValues[2] + "\n" +
                "SET J, " + setupValues[3] + "\n" +
                "SET [0xA000], " + setupValues[4] + "\n" +
                "SET [0xB000], " + setupValues[5] + "\n" +
                "SET [" + String.format("0x%04x", 0xA000 + Integer.parseInt(setupValues[2].substring(2), 16)) + "], " + setupValues[6] + "\n" +
                "SET [" + String.format("0x%04x", 0xB000 + Integer.parseInt(setupValues[3].substring(2), 16)) + "], " + setupValues[7] + "\n" +
                "SET [" + String.format("0x%04x", Integer.parseInt(setupValues[0].substring(2), 16)) + "], " + setupValues[8] + "\n" +
                "SET [" + String.format("0x%04x", Integer.parseInt(setupValues[1].substring(2), 16)) + "], " + setupValues[9] + "\n";
        int i = 0;
        for (String a : aValues) {
            for (String b : bValues) {
                String assembly = setup + cmd + " " + a + ", " + b + "\n" + "HCF 0";
                // System.out.println(assembly);
                dcpu.reset();
                dcpu.memzero();
                dcpu.upload(assembler.assemble(assembly));
                dcpu.run();
                assertExpectedValues(assembly, expected.get(i++), setupValues[0], setupValues[2]);
            }
        }
    }

    private void assertExpectedValues(String assembly, Integer[] expected, String v1, String v2) throws Exception {
        assertEquals("running '" + assembly + "' : A", expected[0].intValue(), (dcpu.getreg(Dcpu.Reg.A)));
        assertEquals("running '" + assembly + "' : EX", expected[1].intValue(), (dcpu.getreg(Dcpu.Reg.EX)));
        assertEquals("running '" + assembly + "' : [0xA000]", expected[2].intValue(), (dcpu.mem[0xA000]));
        assertEquals("running '" + assembly + "' : [0xA010]", expected[3].intValue(), (dcpu.mem[0xA000 + Integer.parseInt(v2.substring(2), 16)]));
        assertEquals("running '" + assembly + "' : [0x0100]", expected[4].intValue(), (dcpu.mem[Integer.parseInt(v1.substring(2), 16)]));
    }

    @Test
    public void testAddOverflow() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET EX, -1\n" +
                        "SET X, 1\n" +
                        "SET Y, 2\n" +
                        "ADD X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 3, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 2, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0, dcpu.getreg(Dcpu.Reg.EX));

        dcpu.reset();
        dcpu.memzero();

        dcpu.upload(assembler.assemble(
                "SET EX, 0\n" +
                        "SET X, 0xfff0\n" +
                        "SET Y, 0x0011\n" +
                        "ADD X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0x11, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 1, dcpu.getreg(Dcpu.Reg.EX));

    }

    public void testSubOverflow() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET EX, 1\n" +
                        "SET X, 1\n" +
                        "SET Y, 2\n" +
                        "SUB Y, X\n"
        ));
        dcpu.run(4);
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0, dcpu.getreg(Dcpu.Reg.EX));

        dcpu.reset();
        dcpu.memzero();
        dcpu.upload(assembler.assemble(
                "SET EX, 0\n" +
                        "SET X, 0\n" +
                        "SET Y, 1\n" +
                        "SUB X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 0xffff, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0xffff, dcpu.getreg(Dcpu.Reg.EX));
    }

    @Test
    public void testMulOverflow() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET EX, 1\n" +
                        "SET X, 3\n" +
                        "SET Y, 4\n" +
                        "MUL X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 12, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 4, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0, dcpu.getreg(Dcpu.Reg.EX));
        dcpu.reset();
        dcpu.memzero();
        char[] bin = assembler.assemble(
                "SET EX, 0\n" +
                        "SET X, 0xffff\n" +
                        "SET Y, 0x10\n" +
                        "MUL X, Y\n"
        );
        char[] expected = new char[]{0x87a1, 0x8061, 0xc481, 0x1064};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run(4);
        assertEquals("x", 0xfff0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0x10, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0x000f, dcpu.getreg(Dcpu.Reg.EX));
    }

    @Test
    public void testDivOverflow() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET EX, 1\n" +
                        "SET X, 12\n" +
                        "SET Y, 4\n" +
                        "DIV X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 3, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 4, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0, dcpu.getreg(Dcpu.Reg.EX));

        dcpu.reset();
        dcpu.memzero();
        dcpu.upload(assembler.assemble(
                "SET EX, 0\n" +
                        "SET X, 0x00ff\n" +
                        "SET Y, 0x1234\n" +
                        "DIV X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 0x0000, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0x1234, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0x0e02, dcpu.getreg(Dcpu.Reg.EX));
    }

    @Test
    public void testModWhenZero() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET EX, 1\n" +
                        "SET X, 12\n" +
                        "SET Y, 0\n" +
                        "MOD X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 1, dcpu.getreg(Dcpu.Reg.EX));
    }

    @Test
    public void testDivWhenZero() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET EX, 1\n" +
                        "SET X, 12\n" +
                        "SET Y, 0\n" +
                        "DIV X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("ex", 0, dcpu.getreg(Dcpu.Reg.EX));
    }

    @Test
    public void testIFEHit() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFE X, 1\n" +
                        "  SET X, 2\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 2, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testIFEMiss() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFE X, 2\n" +
                        "  SET X, 2\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testIFNHit() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFN X, 2\n" +
                        "  SET X, 2\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 2, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testIFNMiss() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFN X, 1\n" +
                        "  SET X, 2\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testIFGHit() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 2\n" +
                        "IFG X, 1\n" +
                        "  SET X, 1\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testIFGMiss() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFG X, 2\n" +
                        "  SET X, -1\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testIFBHit() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFB X, 3\n" +
                        "  SET X, 0\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testIFBMiss() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFB X, 2\n" +
                        "  SET X, 0\n" +
                        "SET Y, 1\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testJSR() throws Exception {
        char[] bin = assembler.assemble(
                "           SET X, 1\n" +
                        "           SET PC, go\n" +
                        ":f1        SET X, 0\n" +
                        "           SET PC, POP\n" +
                        ":go        JSR f1\n" +
                        "           SET Y, 1\n" +
                        "           HCF 0"
        );

        char[] expected = new char[]{0x8861, 0x7f81, 0x0005, 0x8461, 0x6381, 0x7c20, 0x0003, 0x8881, 0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);

        dcpu.run(2);
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("pc", 5, dcpu.getreg(Dcpu.Reg.PC));
        dcpu.run(1);
        assertEquals("pc", 3, dcpu.getreg(Dcpu.Reg.PC));
        dcpu.run(2);
        assertEquals("pc", 7, dcpu.getreg(Dcpu.Reg.PC));
        dcpu.run();
        assertEquals("x", 0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testSimpleJSR() throws Exception {
        char[] bin = assembler.assemble(
                "           JSR go\n" +
                        "           SET X, 1\n" +
                        "           HCF 0\n" +
                        ":go        SET X, 2\n" +
                        "           SET PC, POP\n"
        );
        char[] expected = new char[]{0x7c20, 0x0004, 0x8861, 0x84e0, 0x8c61, 0x6381};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);

        dcpu.upload(bin);

        dcpu.run(1);
        assertEquals("pc", 4, dcpu.getreg(Dcpu.Reg.PC));
        dcpu.run(2);
        assertEquals("pc", 2, dcpu.getreg(Dcpu.Reg.PC));
        assertEquals("x", 2, dcpu.getreg(Dcpu.Reg.X));
        dcpu.run();
        assertEquals("pc", 4, dcpu.getreg(Dcpu.Reg.PC));
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
    }

    @Test
    public void test_PC_SP_Increminting() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET A, 0xFF\n" +
                        "SET A, PC\n" +
                        "SET PUSH, A\n" +
                        "SET B, PEEK\n" +
                        "SET C, SP\n" +
                        "SET I, POP\n" +
                        "SET [0x0100], PC\n" +
                        "SET PUSH, SP\n" +
                        "SET X, 1\n" +
                        "HCF 0"
        ));
        dcpu.run(1);
        dcpu.run(1);
        assertEquals("a", 3, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("pc", 3, dcpu.getreg(Dcpu.Reg.PC));
        assertEquals("sp", 0, dcpu.getreg(Dcpu.Reg.SP));
        dcpu.run(2);
        assertEquals("sp", (char) -1, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("b", 3, dcpu.getreg(Dcpu.Reg.B));
        dcpu.run(1);
        assertEquals("c", (char) -1, dcpu.getreg(Dcpu.Reg.C));
        dcpu.run(1);
        assertEquals("i", 3, dcpu.getreg(Dcpu.Reg.I));
        assertEquals("sp", 0, dcpu.getreg(Dcpu.Reg.SP));
        dcpu.run(1);
        assertEquals("0x0100", 9, dcpu.mem[0x0100]);
        dcpu.run(1);
        assertEquals("sp", (char) -1, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("stack[0]", (char) -1, dcpu.mem[0xffff]);

    }

    @Test
    public void testFibonaciSeriesGeneratedCode() throws Exception {
        dcpu.upload(assembler.assemble(
                ":autoinit\n" +
                        "      SET C, SP\n" +
                        "      SUB C, 256\n" +
                        "\n" +
                        ":autostart\n" +
                        "  JSR main\n" +
                        ":autohalt HCF 0\n" +
                        ":fib\n" +
                        "  SUB  C, 12\n" +
                        "  SET  [10+C], X\n" +
                        "  SET  [8+C], 1\n" +
                        "  SET  [6+C], 1\n" +
                        "  SET  [4+C], 0\n" +
                        "  SET  [0+C], X\n" +
                        ":.LBB0_1\n" +
                        "  SET  J, [4+C]\n" +
                        "  SET  Z, [10+C]\n" +
                        "  SET  EX, 65535\n" +
                        "  IFE  J, Z\n" +
                        "  SET  EX, 0\n" +
                        "  IFG  J, Z\n" +
                        "  SET  EX, 1\n" +
                        "  IFN  EX, 65535\n" +
                        "  SET  PC, .LBB0_4\n" +
                        "  SET  PC, .LBB0_2\n" +
                        ":.LBB0_2\n" +
                        "  SET  J, [8+C]\n" +
                        "  SET  Z, [6+C]\n" +
                        "  ADD  J, Z\n" +
                        "  SET  [2+C], J\n" +
                        "  SET  J, [8+C]\n" +
                        "  SET  [6+C], J\n" +
                        "  SET  J, [2+C]\n" +
                        "  SET  [8+C], J\n" +
                        "  SET  J, [4+C]\n" +
                        "  ADD  J, 1\n" +
                        "  SET  [4+C], J\n" +
                        "  SET  PC, .LBB0_1\n" +
                        ":.LBB0_4\n" +
                        "  SET  X, [8+C]\n" +
                        "  ADD  C, 12\n" +
                        "  SET PC, POP\n" +
                        "\n" +
                        ":main\n" +
                        "  SUB  C, 2\n" +
                        "  SET  [0+C], 0\n" +
                        "  SET  X, 5\n" +
                        "  JSR  fib\n" +
                        "  ADD  C, 2\n" +
                        "  SET PC, POP\n"
        ));
        dcpu.run();
        assertEquals("x", 13, dcpu.getreg(Dcpu.Reg.X));
    }

    @Test
    public void testSettingEX() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET EX, 0xffff\n" +
                        "HCF 0"
        ));
        dcpu.run();
        assertEquals("ex", 0xffff, dcpu.getreg(Dcpu.Reg.EX));
    }

    @Test
    public void testValueOfNW() throws Exception {
        char[] bin = assembler.assemble(
                "SET [0xA000], 0x30\n" +
                        "HCF 0"
        );
        char[] expected = new char[]{0x7fc1, 0x0030, 0xa000, 0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run();
        assertEquals("mem [a0000]", 0x30, (char) (dcpu.mem[0xa000]));
    }

    @Test
    public void testSimpleSHR() throws Exception {
        char[] bin = assembler.assemble(
                "SET A, 0xA234\n" +
                        "SET B, 0x23\n" +
                        "SHR A, B\n" +
                        "HCF 0"
        );
        char[] expected = new char[]{0x7c01, 0xa234, 0x7c21, 0x0023, 0x040d, 0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run();
        assertEquals("a", 0x1446, dcpu.getreg(Dcpu.Reg.A));
    }

    @Test
    public void testMultiBranching() throws Exception {
        char[] bin = assembler.assemble(
                "SET A, 1\n" +
                        "SET B, 1\n" +
                        "IFE A, 2\n" +
                        "IFE B, 1\n" +
                        "    SET X, 1\n" +
                        "SET X, 2\n" +
                        "HCF 0"
        );
        char[] expected = new char[]{0x8801, 0x8821, 0x8c12, 0x8832, 0x8861, 0x8c61, 0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run(3);
        // ensure we skipped all IFX and didn't set X
        assertEquals("pc", 5, dcpu.getreg(Dcpu.Reg.PC));
        assertEquals("x", 0, dcpu.getreg(Dcpu.Reg.X));
    }

    @Test
    public void testMDI() throws Exception {
        char[] bin = assembler.assemble(
                "SET A, -7\n" +
                        "SET B, -16\n" +
                        "MDI A, B\n" +
                        "HCF 0"
        );
        char[] expected = new char[]{0x7c01, 0xfff9, 0x7c21, 0xfff0, 0x0409, 0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run(3);
        assertEquals("a", (char) -7, dcpu.getreg(Dcpu.Reg.A));
    }

    @Test
    public void testHCF() throws Exception {
        char[] bin = assembler.assemble(
                "SET A, 1\n" +
                        "HCF 0\n" +
                        "SET A, 2\n"
        );
        char[] expected = new char[]{0x8801, 0x84e0, 0x8c01};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run();
        assertEquals("a", 1, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("pc", 2, dcpu.getreg(Dcpu.Reg.PC));
    }

    @Test
    public void testStackPICK() throws Exception {
        char[] bin = assembler.assemble(
                "SET [0xfffc], 0xBEEF\n" + // eventual SP - 1
                        "SET A, 1\n" +
                        "SET PUSH, 1\n" +
                        "SET PUSH, 2\n" +
                        "SET PUSH, 3\n" +
                        "SET A, PICK -1\n" +
                        "SET A, PICK 0\n" +
                        "SET A, PICK 1\n" +
                        "SET A, PICK 2\n" +
                        "SET A, PICK 3\n" +
                        "SET A, PICK 4\n" +
                        "HCF 0\n"
        );
        char[] expected = new char[]{
                0x7fc1, 0xbeef, 0xfffc,
                0x8801, 0x8b01, 0x8f01, 0x9301,
                0x6801, 0xffff,
                0x6801, 0x0000,
                0x6801, 0x0001,
                0x6801, 0x0002,
                0x6801, 0x0003,
                0x6801, 0x0004,
                0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run(5);
        assertEquals("A", 1, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffd, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x0001, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0002, dcpu.mem[0xfffe]);
        assertEquals("[SP[3]]", 0x0003, dcpu.mem[0xfffd]);
        dcpu.run(1); // PICK -1 = [0xfffd + (-1)] = [0xfffc]
        assertEquals("A", 0xbeef, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffd, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x0001, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0002, dcpu.mem[0xfffe]);
        assertEquals("[SP[3]]", 0x0003, dcpu.mem[0xfffd]);
        dcpu.run(1); // PICK 0 = [0xfffd] = 3
        assertEquals("A", 3, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffd, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x0001, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0002, dcpu.mem[0xfffe]);
        assertEquals("[SP[3]]", 0x0003, dcpu.mem[0xfffd]);
        dcpu.run(1); // PICK 1 = [0xfffd + 1] = 2
        assertEquals("A", 2, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffd, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x0001, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0002, dcpu.mem[0xfffe]);
        assertEquals("[SP[3]]", 0x0003, dcpu.mem[0xfffd]);
        dcpu.run(1); // PICK 2 = [0xfffd + 2] = 1
        assertEquals("A", 1, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffd, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x0001, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0002, dcpu.mem[0xfffe]);
        assertEquals("[SP[3]]", 0x0003, dcpu.mem[0xfffd]);
        dcpu.run(1); // PICK 3 = [0xfffd + 3]
        assertEquals("A", 0x7fc1, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffd, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x0001, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0002, dcpu.mem[0xfffe]);
        assertEquals("[SP[3]]", 0x0003, dcpu.mem[0xfffd]);
        dcpu.run(1); // PICK 4 = [0xfffd + 4]
        assertEquals("A", 0xbeef, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffd, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x0001, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0002, dcpu.mem[0xfffe]);
        assertEquals("[SP[3]]", 0x0003, dcpu.mem[0xfffd]);
    }

    @Test
    public void testPushPopPick() throws Exception {
        char[] bin = assembler.assemble(
                "SET PUSH, PICK 0\n" + // push first word of program 
                        "SET PUSH, 1\n" +
                        "SET A, PEEK\n" +
                        "SET B, PICK 1\n" +
                        "SET C, POP\n" +
                        "SET X, PICK 1\n" +
                        "HCF 0\n"
        );
        char[] expected = new char[]{
                0x6b01, 0x0000,
                0x8b01,
                0x6401,
                0x6821, 0x0001,
                0x6041,
                0x6861, 0x0001,
                0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run(1); // set push, pick 0
        assertEquals("SP", 0xffff, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x6b01, dcpu.mem[0xffff]);
        dcpu.run(1); // set push, 1
        assertEquals("SP", 0xfffe, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("[SP[1]]", 0x6b01, dcpu.mem[0xffff]);
        assertEquals("[SP[2]]", 0x0001, dcpu.mem[0xfffe]);
        dcpu.run(1); // set a, peek
        assertEquals("A", 0x0001, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("SP", 0xfffe, dcpu.getreg(Dcpu.Reg.SP));
        dcpu.run(1); // set b, pick 1
        assertEquals("B", 0x6b01, dcpu.getreg(Dcpu.Reg.B));
        assertEquals("SP", 0xfffe, dcpu.getreg(Dcpu.Reg.SP));
        dcpu.run(1); // set c, pop
        assertEquals("C", 0x0001, dcpu.getreg(Dcpu.Reg.C));
        assertEquals("SP", 0xffff, dcpu.getreg(Dcpu.Reg.SP));
        dcpu.run(1); // set X, pick 1
        assertEquals("X", 0x6b01, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("SP", 0xffff, dcpu.getreg(Dcpu.Reg.SP));
    }

    @Test
    public void testDVI() throws Exception {
        assertDVI(16, 7, (char) 0x4924, new char[]{0xc401, 0xa021, 0x0407, 0x84e0});
        assertDVI(-16, 7, (char) 0xb6dc, new char[]{0x7c01, 0xfff0, 0xa021, 0x0407, 0x84e0});
        assertDVI(16, -7, (char) 0xb6dc, new char[]{0xc401, 0x7c21, 0xfff9, 0x0407, 0x84e0});
        assertDVI(-16, -7, (char) 0x4924, new char[]{0x7c01, 0xfff0, 0x7c21, 0xfff9, 0x0407, 0x84e0});
    }

    private void assertDVI(int aValue, int bValue, char expectedValue, char[] expectedBin) throws ArrayComparisonFailure {
        char[] bin = assembler.assemble(
                "SET A, " + aValue + "\n" +
                        "SET B, " + bValue + "\n" +
                        "DVI A, B\n" +
                        "HCF 0\n"
        );

        assertArrayEquals(TestUtils.displayExpected(expectedBin, bin), expectedBin, bin);
        dcpu.reset();
        dcpu.memzero();
        dcpu.upload(bin);
        dcpu.run();
        assertEquals("ex", expectedValue, dcpu.getreg(Dcpu.Reg.EX));
    }

    @Test
    public void testIagIas() throws Exception {
        char[] bin = assembler.assemble(
                "SET A, 0x30\n" +
                        "IAS A\n" +
                        "SET A, 0x0\n" +
                        "IAG A\n" +
                        "HCF 0"
        );
        char[] expected = new char[]{0x7c01, 0x0030, 0x0140, 0x8401, 0x0120, 0x84e0};
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);
        dcpu.upload(bin);
        dcpu.run(2);
        assertEquals("ia", 0x30, dcpu.getreg(Dcpu.Reg.IA));
        dcpu.run(2);
        assertEquals("a", 0x30, dcpu.getreg(Dcpu.Reg.A));
    }

}
