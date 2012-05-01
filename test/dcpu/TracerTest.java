package dcpu;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class TracerTest {
    private Assembler assembler;
    private Dcpu dcpu;
    private Tracer tracer;

    @Before
    public void setUp() {
        assembler = new Assembler();
        dcpu = new Dcpu();
    }

    @Test
    public void testTraceNonBranchingSingleWordCommands() throws Exception {
        char[] bin = assembler.assemble(
                "SET a, 1\n" +
                        "ADD a, 1\n" +
                        "SUB a, 1\n" +
                        "MUL a, 1\n" +
                        "DIV a, 1\n" +
                        "MOD a, 1\n" +
                        "SHL a, 1\n" +
                        "SHR a, 1\n" +
                        "AND a, 1\n" +
                        "BOR a, 1\n" +
                        "XOR a, 1\n" +
                        "HCF 0"
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        tracer = new Tracer(new PrintStream(baos));
        dcpu.upload(bin);
        tracer.install(dcpu);
        dcpu.run();
        String expected =
                "0000: SET A, 1\n" +
                        "0001: ADD A, 1\n" +
                        "0002: SUB A, 1\n" +
                        "0003: MUL A, 1\n" +
                        "0004: DIV A, 1\n" +
                        "0005: MOD A, 1\n" +
                        "0006: SHL A, 1\n" +
                        "0007: SHR A, 1\n" +
                        "0008: AND A, 1\n" +
                        "0009: BOR A, 1\n" +
                        "000a: XOR A, 1\n" +
                        "000b: HCF 0\n";
        assertEquals("trace", expected, baos.toString());
    }

    @Test
    public void testMultipleWordCommands() throws Exception {
        char[] bin = assembler.assemble(
                "SET [0x0800], 1\n" +
                        "ADD a, [0x8000]\n" +
                        "SUB [0x8000 + i], 1\n" +
                        "MUL [0x8000 + i], [0x9000 + i]\n" +
                        "DIV 0x8000, 1\n" +
                        "MOD a, 0x8000\n" +
                        "SHL 0x8000, 0x8000\n" +
                        "HCF 0"
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        tracer = new Tracer(new PrintStream(baos));
        dcpu.upload(bin);
        tracer.install(dcpu);
        dcpu.run();
        String expected =
                "0000: SET [0x0800], 1\n" +
                        "0002: ADD A, [0x8000]\n" +
                        "0004: SUB [I + 0x8000], 1\n" +
                        "0006: MUL [I + 0x8000], [I + 0x9000]\n" +
                        "0009: DIV 0x8000, 1\n" +
                        "000b: MOD A, 0x8000\n" +
                        "000d: SHL 0x8000, 0x8000\n" +
                        "0010: HCF 0\n";
        assertEquals("trace", expected, baos.toString());
    }

    @Test
    public void testLabelsOnNonBranching() throws Exception {
        char[] bin = assembler.assemble(
                ":on_line      SET [0x0800], 1\n" +
                        "              ADD a, [0x8000]\n" +
                        ":before_sub\n" +
                        "              SUB [0x8000 + i], 1\n" +
                        "              MUL [0x8000 + i], [0x9000 + i]\n" +
                        ":one_of_two\n" +
                        ":two_of_two\n" +
                        "              DIV 0x8000, 1\n" +
                        "              MOD a, 0x8000\n" +
                        "              SHL 0x8000, 0x8000\n" +
                        "              HCF 0\n" +
                        ":at_end"
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        tracer = new Tracer(new PrintStream(baos));
        dcpu.upload(bin);
        tracer.install(dcpu);
        dcpu.run();
        String expected =
                "0000: SET [0x0800], 1\n" +
                        "0002: ADD A, [0x8000]\n" +
                        "0004: SUB [I + 0x8000], 1\n" +
                        "0006: MUL [I + 0x8000], [I + 0x9000]\n" +
                        "0009: DIV 0x8000, 1\n" +
                        "000b: MOD A, 0x8000\n" +
                        "000d: SHL 0x8000, 0x8000\n" +
                        "0010: HCF 0\n";
        assertEquals("trace", expected, baos.toString());
    }

    @Test
    public void testBranchingCommandsThatHit() throws Exception {
        char[] bin = assembler.assemble(
                "; positive hits on all IFX\n" +
                        "SET a, 2\n" +
                        "IFE a, 2\n" +
                        "    SET a, 3\n" +
                        "SET i, 1\n" +
                        "IFN i, 2\n" +
                        "    SET i, 3\n" +
                        "SET a, 1\n" +
                        "IFG a, 0\n" +
                        "    SET J, 2\n" +
                        "IFB J, 2\n" +
                        "    SET J, 1\n" +
                        "HCF 0"
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        tracer = new Tracer(new PrintStream(baos));
        dcpu.upload(bin);
        tracer.install(dcpu);
        dcpu.run();
        String expected =
                "0000: SET A, 2\n" +
                        "0001: IFE A, 2\n" +
                        "0002: SET A, 3\n" +
                        "0003: SET I, 1\n" +
                        "0004: IFN I, 2\n" +
                        "0005: SET I, 3\n" +
                        "0006: SET A, 1\n" +
                        "0007: IFG A, 0\n" +
                        "0008: SET J, 2\n" +
                        "0009: IFB J, 2\n" +
                        "000a: SET J, 1\n" +
                        "000b: HCF 0\n";
        assertEquals("trace", expected, baos.toString());
    }

    @Test
    public void testBranchingCommandsThatMiss() throws Exception {
        // this is the one that was broken - it was printing the missed statement instead of the condition statement
        char[] bin = assembler.assemble(
                "; and now negative hits on all IFX\n" +
                        "SET a, 1\n" +
                        "IFE a, 2\n" +
                        "    SET a, 3\n" +
                        "SET i, 2\n" +
                        "IFN i, 2\n" +
                        "    SET i, 3\n" +
                        "SET a, 0\n" +
                        "IFG a, 1\n" +
                        "    SET J, 2\n" +
                        "SET J, 0\n" +
                        "IFB J, 0\n" +
                        "    SET J, 1\n" +
                        "HCF 0"
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        tracer = new Tracer(new PrintStream(baos));
        dcpu.upload(bin);
        tracer.install(dcpu);
        dcpu.run();
        String expected =
                "0000: SET A, 1\n" +
                        "0001: IFE A, 2\n" +
                        "0003: SET I, 2\n" +
                        "0004: IFN I, 2\n" +
                        "0006: SET A, 0\n" +
                        "0007: IFG A, 1\n" +
                        "0009: SET J, 0\n" +
                        "000a: IFB J, 0\n" +
                        "000c: HCF 0\n";
        assertEquals("trace", expected, baos.toString());
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        tracer = new Tracer(new PrintStream(baos));
        dcpu.upload(bin);
        tracer.install(dcpu);
        dcpu.run();
        String expected =
                "0000: SET [0xfffc], 0xbeef\n" +
                        "0003: SET A, 1\n" +
                        "0004: SET PUSH, 1\n" +
                        "0005: SET PUSH, 2\n" +
                        "0006: SET PUSH, 3\n" +
                        //"0007: SET A, PICK -1\n" +
                        "0007: SET A, PICK 65535\n" +
                        "0009: SET A, PICK 0\n" +
                        "000b: SET A, PICK 1\n" +
                        "000d: SET A, PICK 2\n" +
                        "000f: SET A, PICK 3\n" +
                        "0011: SET A, PICK 4\n" +
                        "0013: HCF 0\n";
        assertEquals("trace", expected, baos.toString());
    }
}
