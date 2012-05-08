package dcpu;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import static dcpu.Debugger.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DebuggerTest {
    private Dcpu cpu;
    private Debugger debugger;
    private AntlrAssembler assembler;

    @Before
    public void setUp() {
        cpu = new Dcpu();
        debugger = new Debugger();
        debugger.attachTo(cpu);
        assembler = new AntlrAssembler();
        assembler.setGenerateMap(true);
    }

    @Test
    public void testBreakpoints() throws Exception {
        cpu.upload(assembler.assemble(
                "       SET A,1\n" +
                        "       IFE A,0\n" +
                        ":nobrk    SET A,2\n" +
                        "       SET A,3\n" +
                        ":brk   SET A,4\n" +
                        "       SET A,5\n" +
                        "       SET A,6\n" +
                        "       DAT 0\n"));

        char nobrk = assembler.getAsmMap().symbol("nobrk");
        final char brk = assembler.getAsmMap().symbol("brk");
        debugger.setBreakpoint(nobrk, true);
        debugger.setBreakpoint(brk, true);
        // sanity check
        assertEquals(2, debugger.getBreakpoints().size());
        assertTrue(debugger.getBreakpoints().contains(nobrk));
        assertTrue(debugger.getBreakpoints().contains(brk));

        final List<Character> brkHits = new ArrayList<Character>();
        debugger.breakpointsHalt = false;
        debugger.breakpointListener = new PreListener<Character>() {
            @Override
            public void preExecute(Character pc) {
                brkHits.add(pc);
            }
        };
        debugger.resetSession();
        debugger.run();
        assertEquals("brk count", 1, brkHits.size());
        assertEquals("brk pt", (Object) brk, brkHits.get(0));
    }

    @Test
    public void testModifiedRegisters() {
        cpu.upload(assembler.assemble(
                "      SET X,0\n" +
                        ":brk1 SET A,1\n" +
                        "      SET B,2\n" +
                        "      SET PUSH,B\n" +
                        ":brk2 SET C,3\n" +
                        "      SET Y,POP\n" +
                        "      DAT 0\n"));
        Character brk1 = assembler.getAsmMap().symbol("brk1");
        Character brk2 = assembler.getAsmMap().symbol("brk2");
        final LinkedList<BitSet> modregs_list = new LinkedList<BitSet>();

        debugger.setBreakpoint(brk1, true);
        debugger.setBreakpoint(brk2, true);
        debugger.breakpointsHalt = false;
        BitSet modregs;
        Listener<Character> recorder = new PreListener<Character>() {
            @Override
            public void preExecute(Character arg) {
                modregs_list.add(debugger.getModifiedRegisters());
            }
        };

        // PASS #1: calculate modregs only on breakpoints
        modregs_list.clear();
        debugger.breakpointListener = recorder;
        debugger.modregsOnBreakpoint = true;
        debugger.modregsOnRun = false;
        debugger.modregsOnStep = false;
        debugger.resetSession();
        debugger.run();
        assertEquals(2, modregs_list.size());
        // from start to brk1 only PC changed
        modregs = modregs_list.pop();
        assertEquals(1, modregs.cardinality());
        assertTrue(modregs.get(DREG_PC));
        // SET A,1 ; SET B,2 ; SET PUSH,B ; :brk2
        // from brk1 to brk2 were modified: A, B, SP, PC
        modregs = modregs_list.pop();
        assertEquals(4, modregs.cardinality());
        assertTrue(modregs.get(DREG_A));
        assertTrue(modregs.get(DREG_B));
        assertTrue(modregs.get(DREG_SP));
        assertTrue(modregs.get(DREG_PC));

        // PASS #2: calculate modregs on each step
        modregs_list.clear();
        debugger.breakpointListener = recorder;
        debugger.modregsOnBreakpoint = false;
        debugger.modregsOnRun = true;
        debugger.modregsOnStep = false;
        debugger.resetSession();
        debugger.run();
        assertEquals(2, modregs_list.size());
        // from start to brk1 only PC changed
        modregs = modregs_list.pop();
        assertEquals(1, modregs.cardinality());
        assertTrue(modregs.get(DREG_PC));
        // before brk2 only PC and SP were modified by SET PUSH,B
        modregs = modregs_list.pop();
        assertEquals(2, modregs.cardinality());
        assertTrue(modregs.get(DREG_SP));
        assertTrue(modregs.get(DREG_PC));

        // PASS #3. Simulating debugger. Calculate modregs only on breakpoints and
        // manual steps.
        // We will run to second breakpoint, then step twice
        modregs_list.clear();
        debugger.breakpointListener = null;
        debugger.modregsOnBreakpoint = true;
        debugger.modregsOnRun = false;
        debugger.modregsOnStep = true;
        debugger.breakpointsHalt = true;
        debugger.resetSession();
        debugger.run();// :brk1
        debugger.run();// SET A,1 ; SET B,2 ; SET PUSH,B ; :brk2
        modregs = debugger.getModifiedRegisters();
        assertEquals(4, modregs.cardinality());
        assertTrue(modregs.get(DREG_A));
        assertTrue(modregs.get(DREG_B));
        assertTrue(modregs.get(DREG_SP));
        assertTrue(modregs.get(DREG_PC));
        debugger.step(); //SET C,3
        modregs = debugger.getModifiedRegisters();
        assertEquals(2, modregs.cardinality());
        assertTrue(modregs.get(DREG_C));
        assertTrue(modregs.get(DREG_PC));
        debugger.step(); //SET Y,POP
        modregs = debugger.getModifiedRegisters();
        assertEquals(3, modregs.cardinality());
        assertTrue(modregs.get(DREG_Y));
        assertTrue(modregs.get(DREG_SP));
        assertTrue(modregs.get(DREG_PC));
    }
}
