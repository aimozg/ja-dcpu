package dcpu;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
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
        expected.add(new Integer[]{0xff00, 0x0001, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0080, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x00a0, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x00c0, 0x0000, 0x0030, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0001, 0x0030, 0x0050, 0xfe70});
        expected.add(new Integer[]{0x0100, 0x0001, 0x0030, 0x0050, 0xfff0});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x0010});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0050, 0x0030});
        expected.add(new Integer[]{0x0100, 0x0001, 0x0030, 0xfe50, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0001, 0x0030, 0xffd0, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0001, 0x0030, 0xfff0, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0000, 0x0030, 0x0010, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0001, 0xfe30, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0001, 0xffb0, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0001, 0xffd0, 0x0050, 0x0070});
        expected.add(new Integer[]{0x0100, 0x0001, 0xfff0, 0x0050, 0x0070});
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
        expected.add(new Integer[]{0x1446, 0x0004, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0000, 0x0030, 0x0050, 0x5a5a});
        expected.add(new Integer[]{0xa234, 0x0002, 0x0030, 0x0050, 0x0b4b});
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
                String assembly = setup + cmd + " " + a + ", " + b + "\n";
                // System.out.println(assembly);
                dcpu.reset();
                dcpu.upload(assembler.assemble(assembly));
                dcpu.run(11);
                assertExpectedValues(expected.get(i++), setupValues[0], setupValues[2]);
            }
        }
    }

    private void assertExpectedValues(Integer[] expected, String v1, String v2) throws Exception {
        assertEquals("A", expected[0].shortValue(), (short) (dcpu.getreg(Dcpu.Reg.A) & 0xffff));
        assertEquals("O", expected[1].shortValue(), (short) (dcpu.getreg(Dcpu.Reg.O) & 0xffff));
        assertEquals("[0xA000]", expected[2].shortValue(), (short) (dcpu.mem[0xA000] & 0xffff));
        assertEquals("[0xA010]", expected[3].shortValue(), (short) (dcpu.mem[0xA000 + Integer.parseInt(v2.substring(2), 16)] & 0xffff));
        assertEquals("[0x0100]", expected[4].shortValue(), (short) (dcpu.mem[Integer.parseInt(v1.substring(2), 16)] & 0xffff));
    }

    @Test
    public void testAddOverflow() throws Exception {
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

    public void testSubOverflow() throws Exception {
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
    public void testMulOverflow() throws Exception {
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
    public void testDivOverflow() throws Exception {
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

    @Test
    public void testModWhenZero() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET O, 1\n" +
                        "SET X, 12\n" +
                        "SET Y, 0\n" +
                        "MOD X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", 0, dcpu.getreg(Dcpu.Reg.O));
    }

    @Test
    public void testDivWhenZero() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET O, 1\n" +
                        "SET X, 12\n" +
                        "SET Y, 0\n" +
                        "DIV X, Y\n"
        ));
        dcpu.run(4);
        assertEquals("x", 0, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 0, dcpu.getreg(Dcpu.Reg.Y));
        assertEquals("o", 0, dcpu.getreg(Dcpu.Reg.O));
    }

    @Test
    public void testIFEHit() throws Exception {
        dcpu.upload(assembler.assemble(
                "SET X, 1\n" +
                        "IFE X, 1\n" +
                        "  SET X, 2\n" +
                        "SET Y, 1\n" +
                        "HLT"
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
                        "HLT"
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
                        "HLT"
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
                        "HLT"
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
                        "HLT"
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
                        "HLT"
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
                        "HLT"
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
                        "HLT"
        ));
        dcpu.run();
        assertEquals("x", 1, dcpu.getreg(Dcpu.Reg.X));
        assertEquals("y", 1, dcpu.getreg(Dcpu.Reg.Y));
    }

    @Test
    public void testJSR() throws Exception {
        dcpu.upload(assembler.assemble(
                "           SET X, 1\n" +
                        "           SET PC, go\n" +
                        ":f1        SET X, 0\n" +
                        "           SET PC, POP\n" +
                        ":go        JSR f1\n" +
                        "           SET Y, 1\n" +
                        "HLT"
        ));
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
                "HLT"
        ));
        dcpu.run(1);
        dcpu.run(1);
        assertEquals("a", 3, dcpu.getreg(Dcpu.Reg.A));
        assertEquals("pc", 3, dcpu.getreg(Dcpu.Reg.PC));
        assertEquals("sp", 0, dcpu.getreg(Dcpu.Reg.SP));
        dcpu.run(2);
        assertEquals("sp", -1, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("b", 3, dcpu.getreg(Dcpu.Reg.B));
        dcpu.run(1);
        assertEquals("c", -1, dcpu.getreg(Dcpu.Reg.C));
        dcpu.run(1);
        assertEquals("i", 3, dcpu.getreg(Dcpu.Reg.I));
        assertEquals("sp", 0, dcpu.getreg(Dcpu.Reg.SP));
        dcpu.run(1);
        assertEquals("0x0100", 9, dcpu.mem[0x0100]);
        dcpu.run(1);
        assertEquals("sp", -1, dcpu.getreg(Dcpu.Reg.SP));
        assertEquals("stack[0]", -1, dcpu.mem[0xffff]);
    }

    @Ignore("Broken code that works on dcpu.ru")
    @Test
    public void testFibonaciSeriesGeneratedCode() throws Exception {
        // this is some generated code that works on web based assembler/emulators
        // and was generated by https://github.com/llvm-dcpu16/llvm-dcpu16.
        // right now, it doesn't work on ours
        dcpu.upload(assembler.assemble(
            ":autoinit\n" +
            "      SET C, SP\n" +
            "      SUB C, 256\n" +
            "\n" +
            ":autostart\n" +
            "  JSR main\n" +
            ":autohalt HLT\n" +
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
            "  SET  O, 65535\n" +
            "  IFE  J, Z\n" +
            "  SET  O, 0\n" +
            "  IFG  J, Z\n" +
            "  SET  O, 1\n" +
            "  IFN  O, 65535\n" +
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
        assertEquals("x", 5, dcpu.getreg(Dcpu.Reg.X));
    }
}
