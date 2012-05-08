package dcpu;

import org.junit.Before;
import org.junit.Test;

import static dcpu.Dcpu.Reg;
import static org.junit.Assert.assertEquals;

public class InterruptTest {
    private Dcpu cpu;
    private AntlrAssembler assembler;
    private AsmMap map;

    @Before
    public void setUp() {
        cpu = new Dcpu();
        assembler = new AntlrAssembler();
        assembler.setGenerateMap(true);
    }

    @Test
    public void testSimpleSoftInterrupt() {
        asm("" +
                "   IAS inthandler\n" +
                "   SET B,1\n" +
                "   INT 2\n" +
                "   SET B,3\n" +
                "   HCF 0\n" +
                ":inthandler\n" +
                "   HCF 0\n"
        );
        cpu.run(4); // IAS, SET, INT, <int>
        assertEquals(1, cpu.getreg(Reg.B));
        assertEquals(2, cpu.getreg(Reg.A));
        assertEquals(map.symbol("inthandler").charValue(), cpu.getreg(Reg.PC));//don't use cpu.pc() - it returns unsigned
    }

    @Test
    public void testInterruptStack() {
        asm("" +
                "   IAS inthandler\n" +
                "   SET A,0xface\n" +
                "   INT 1\n" +
                ":afterint\n" +
                "   HCF 0\n" +
                ":inthandler\n" +
                "   HCF 0\n");
        cpu.run(4); // IAS, SET, INT, <int>
        assertEquals(0xface, cpu.mem[cpu.sp()]);
        assertEquals(map.symbol("afterint").charValue(), cpu.mem[cpu.sp() + 1]);
    }

    @Test
    public void testSimpleHardInterrupt() {
        asm("" +
                "   IAS inthandler\n" +
                "   SET B,1\n" +
                "   SET B,2\n" +
                "   HCF 0\n" +
                ":inthandler\n" +
                "   HCF 0\n"
        );
        cpu.run(2); // IAS, SET 1
        cpu.interrupt((char) 3);
        cpu.step(false); // <int>
        assertEquals(1, cpu.getreg(Reg.B));
        assertEquals(3, cpu.getreg(Reg.A));
        assertEquals(map.symbol("inthandler").charValue(), cpu.pc());
    }

    @Test
    public void testRFI() {
        asm("" +
                "   IAS inthandler\n" +
                "   SET A,3\n" +
                "   INT 2\n" +
                ":afterint\n" +
                "   HCF 0\n" +
                ":inthandler\n" +
                "   RFI 0\n"
        );
        cpu.run(3);// IAS, SET, INT
        int oldSp = cpu.sp();
        cpu.run(1);//<int>
        assertEquals(2, cpu.getreg(Reg.A));
        cpu.step(false);//RFI
        assertEquals(map.symbol("afterint").charValue(), cpu.pc());
        assertEquals(oldSp, cpu.sp());
        assertEquals(3, cpu.getreg(Reg.A));
    }

    @Test
    public void testIA0() {
        asm("" +
                "   SET B,1\n" +
                "   INT 2\n" +
                "   SET B,2\n" +
                ":run3\n" +
                "   HCF 0\n");
        cpu.run(3);// SET, INT, SET
        // if there were an interrupt, 3rd call would result in <int>, and B would be 1
        assertEquals(2, cpu.getreg(Reg.B));
        assertEquals(map.symbol("run3").charValue(), cpu.pc());
    }

    // TODO maybe extract this helper method, cpu, assembler, and 'map' fields to some kind of DcpuTest superclass?
    private void asm(String src) {
        assembler.reset();
        cpu.upload(assembler.assemble(src));
        cpu.reset();
        map = assembler.getAsmMap();
    }

}
