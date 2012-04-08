package dcpu;

import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static dcpu.Dcpu.*;

public class Debugger {

    /**
     * TRUE if breakpoints are enabled
     */
    public boolean breakpointsEnabled = true;
    /**
     * TRUE if breakpoints halt the DCPU (otherwise just breakpoint handler will be called)
     */
    public boolean breakpointsHalt = true;

    /**
     * Executed each step. Argument = PC of the instruction
     */
    public Listener<Short> stepListener = null;
    /**
     * Executed when breakpoint is hit. Argument = PC of the instruction
     */
    public Listener<Short> breakpointListener = null;
    /**
     * Calculate modregs on each manual step
     */
    public boolean modregsOnStep = true;
    /**
     * Calculate modregs on breakpoint hit
     */
    public boolean modregsOnBreakpoint = true;
    /**
     * Calculate modregs on each CPU instruction
     */
    public boolean modregsOnRun = false;

    private boolean onehitModregs;

    /**
     * Returns set of registers, modified on previous saved point. For indices see DREG_xxx constants.
     * <p/>
     * About configuration of modregs flags:
     * * By setting modregsOnBreakpoint only, you will get set of changed registers between breakpoint hits
     * * By setting modregsOnStep, you will get registers changed when doing manual step
     * * By setting modregsOnRun, only changes from previous instruction are saved
     */
    public BitSet getModifiedRegisters() {
        return modifiedRegisters.get(0, REGS_COUNT);
    }

    public static final int DREG_A = M_A - M_A;
    public static final int DREG_B = M_B - M_A;
    public static final int DREG_C = M_C - M_A;
    public static final int DREG_X = M_X - M_A;
    public static final int DREG_Y = M_Y - M_A;
    public static final int DREG_Z = M_Z - M_A;
    public static final int DREG_I = M_I - M_A;
    public static final int DREG_J = M_J - M_A;
    public static final int DREG_SP = M_SP - M_A;
    public static final int DREG_PC = M_PC - M_A;
    public static final int DREG_O = M_O - M_A;

    private short[] oldRegs = new short[REGS_COUNT];
    private BitSet modifiedRegisters = new BitSet(REGS_COUNT);
    private Dcpu cpu;
    private Set<Short> breakpoints = new LinkedHashSet<Short>();

    /**
     * Sets (brk=true) or releases (brk=false) breakpoint on instruction with specific address.
     * <p/>
     * Breakpoints will fire BEFORE the execution of instruction
     */
    public void setBreakpoint(short address, boolean brk) {
        if (brk) {
            breakpoints.add(address);
        } else {
            breakpoints.remove(address);
        }
    }

    public Set<Short> getBreakpoints() {
        return Collections.unmodifiableSet(breakpoints);
    }

    public void resetSession() {
        cpu.reset();
        modifiedRegisters.clear();
        System.arraycopy(cpu.mem, M_A, oldRegs, 0, REGS_COUNT);
    }

    public void step() {
        onehitModregs = modregsOnStep;
        cpu.step(false);
    }

    public void run() {
        cpu.run();
    }

    private void stepHandler(Short pc) {
        if (onehitModregs || modregsOnRun) updModRegs();
        if (breakpointsEnabled && breakpoints.contains(pc)) {
            if (modregsOnBreakpoint) updModRegs();
            if (breakpointListener != null) breakpointListener.event(pc);
            cpu.halt = breakpointsHalt;
        }
        if (stepListener != null) stepListener.event(pc);
    }

    private void updModRegs() {
        modifiedRegisters.clear();
        for (int i = 0; i < REGS_COUNT; i++) {
            if (cpu.mem[M_A + i] != oldRegs[i]) {
                modifiedRegisters.set(i);
                oldRegs[i] = cpu.mem[M_A + i];
            }
        }
    }

    public void attachTo(Dcpu cpu) {
        if (this.cpu != null) detach();
        this.cpu = cpu;
        cpu.stepListener = new Listener<Short>() {
            public void event(Short arg) {
                Debugger.this.stepHandler(arg);
            }
        };
    }

    public void detach() {
        cpu.stepListener = null;
    }

}
