package dcpu;

import java.io.PrintStream;

import static dcpu.Dcpu.*;

/**
 * Prints commands to PrintStream after they have been executed
 */
public class Tracer extends PostListener<Character> {

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
    public void postExecute(Character pc) {
        printTrace(pc);
    }

    private void printTrace(char pc) {
        disassembler.setAddress(pc);
        out.printf("%04x: %s\n", (int) pc, disassembler.next(true));
        if (printRegisters)
            Tracer.outputRegisters(out, dcpu);
        if (printMemAtReg) {
            Tracer.outputRegMem(out, dcpu);
        }
        if (printStack > 0) {
            int sp = dcpu.mem[M_SP];
            out.printf("  S: ");
            for (int i = 0; i < printStack; i++) {
                out.printf(" %04x", (int) dcpu.mem[sp]);
                sp = (sp + 1) % 0x10000;
            }
            out.println();
        }
    }

    public static void outputRegMem(PrintStream out, Dcpu dcpu) {
        outputRegMem(out,
                dcpu.mem[dcpu.mem[M_A]], dcpu.mem[dcpu.mem[M_B]], dcpu.mem[dcpu.mem[M_C]],
                dcpu.mem[dcpu.mem[M_X]], dcpu.mem[dcpu.mem[M_Y]], dcpu.mem[dcpu.mem[M_Z]],
                dcpu.mem[dcpu.mem[M_I]], dcpu.mem[dcpu.mem[M_J]],
                dcpu.mem[dcpu.mem[M_SP]], dcpu.mem[dcpu.mem[M_EX]]);
    }

    public static void outputRegMem(PrintStream out, char aM, char bM, char cM, char xM, char yM, char zM, char iM, char jM, char spM, char oM) {
        out.printf("  M:  A*%04x B*%04x C*%04x X*%04x Y*%04x Z*%04x I*%04x J*%04x  SP*%04x EX*%04x\n",
                (int) aM, (int) bM, (int) cM, (int) xM, (int) yM, (int) zM, (int) iM, (int) jM, (int) spM, (int) oM);
    }

    public static void outputRegisters(PrintStream out, Dcpu dcpu) {
        outputRegisters(out,
                dcpu.mem[M_A], dcpu.mem[M_B], dcpu.mem[M_C],
                dcpu.mem[M_X], dcpu.mem[M_Y], dcpu.mem[M_Z],
                dcpu.mem[M_I], dcpu.mem[M_J],
                dcpu.mem[M_SP], dcpu.mem[M_EX]);
    }

    public static void outputRegisters(PrintStream out, char A, char B, char C, char X, char Y, char Z, char I, char J, char SP, char O) {
        out.printf("  R:  A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x  SP=%04x EX=%04x\n",
                (int) A, (int) B, (int) C, (int) X, (int) Y, (int) Z, (int) I, (int) J, (int) SP, (int) O);
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

    public boolean getPrintRegisters() {
        return printRegisters;
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
