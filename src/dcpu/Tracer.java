package dcpu;

import java.io.PrintStream;

import static dcpu.Dcpu.*;

/**
 * Prints commands to PrintStream after they have been executed
 */
public class Tracer extends PostListener<Short> {

    Dcpu dcpu;
    Disassembler disassembler;
    private boolean printRegisters = false;
    private int printStack = 0;
    private boolean printMemAtReg = false;
    PrintStream out;

    public Tracer(PrintStream out) {
        this.out = out;
    }

    public void install(Dcpu dcpu) {
        this.dcpu = dcpu;
        dcpu.stepListener = this;
        disassembler = new Disassembler();
        disassembler.init(dcpu.mem);
    }

    @Override
    public void postExecute(Short pc) {
        printTrace(pc);
    }

    private void printTrace(Short pc) {
        disassembler.setAddress(pc & 0xffff);
        out.printf("%04x: %s\n", pc, disassembler.next(true));
        if (printRegisters)
            Tracer.outputRegisters(out, dcpu);
        if (printMemAtReg) {
            Tracer.outputRegMem(out, dcpu);
        }
        if (printStack > 0) {
            int sp = dcpu.mem[M_SP] & 0xffff;
            out.printf("  S: ");
            for (int i = 0; i < printStack; i++) {
                out.printf(" %04x", dcpu.mem[sp]);
                sp = (sp + 1) % 0x10000;
            }
            out.println();
        }
    }

    public static void outputRegMem(PrintStream out, Dcpu dcpu) {
        outputRegMem(out,
                dcpu.mem[0xffff & dcpu.mem[M_A]], dcpu.mem[0xffff & dcpu.mem[M_B]], dcpu.mem[0xffff & dcpu.mem[M_C]],
                dcpu.mem[0xffff & dcpu.mem[M_X]], dcpu.mem[0xffff & dcpu.mem[M_Y]], dcpu.mem[0xffff & dcpu.mem[M_Z]],
                dcpu.mem[0xffff & dcpu.mem[M_I]], dcpu.mem[0xffff & dcpu.mem[M_J]],
                dcpu.mem[0xffff & dcpu.mem[M_SP]], dcpu.mem[0xffff & dcpu.mem[M_EX]]);
    }

    public static void outputRegMem(PrintStream out, short aM, short bM, short cM, short xM, short yM, short zM, short iM, short jM, short spM, short oM) {
        out.printf("  M:  A*%04x B*%04x C*%04x X*%04x Y*%04x Z*%04x I*%04x J*%04x  SP*%04x EX*%04x\n", aM, bM, cM, xM, yM, zM, iM, jM, spM, oM);
    }

    public static void outputRegisters(PrintStream out, Dcpu dcpu) {
        outputRegisters(out,
                dcpu.mem[M_A], dcpu.mem[M_B], dcpu.mem[M_C],
                dcpu.mem[M_X], dcpu.mem[M_Y], dcpu.mem[M_Z],
                dcpu.mem[M_I], dcpu.mem[M_J],
                dcpu.mem[M_SP], dcpu.mem[M_EX]);
    }

    public static void outputRegisters(PrintStream out, short A, short B, short C, short X, short Y, short Z, short I, short J, short SP, short O) {
        out.printf("  R:  A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x  SP=%04x EX=%04x\n", A, B, C, X, Y, Z, I, J, SP, O);
    }

    public void printRegisters(boolean b) {
        this.printRegisters = b;
    }

    public void printStack(int i) {
        this.printStack = i;
    }

    public void printMemAtReg(boolean b) {
        this.printMemAtReg = b;
    }

    public boolean togglePrintRegisters() {
        printRegisters = !printRegisters;
        return printRegisters;
    }

    public boolean togglePrintMemAtReg() {
        printMemAtReg = !printMemAtReg;
        return printMemAtReg;
    }
}
