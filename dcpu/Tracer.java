package dcpu;

import java.io.PrintStream;

import static dcpu.Dcpu.*;

/**
 * Prints executed commands to PrintStream
 */
public class Tracer implements Listener<Short> {

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

    public void event(Short pc) {
        disassembler.setAddress(pc & 0xffff);
        out.printf("%04x: %s\n", pc, disassembler.next());
        if (printRegisters)
            out.printf("  R:  A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x  SP=%04x O=%04x\n",
                    dcpu.mem[M_A], dcpu.mem[M_B], dcpu.mem[M_C],
                    dcpu.mem[M_X], dcpu.mem[M_Y], dcpu.mem[M_Z],
                    dcpu.mem[M_I], dcpu.mem[M_J],
                    dcpu.mem[M_SP], dcpu.mem[M_O]);
        if (printMemAtReg)
            out.printf("  M:  A*%04x B*%04x C*%04x X*%04x Y*%04x Z*%04x I*%04x J*%04x  SP*%04x O*%04x\n",
                    dcpu.mem[0xffff & dcpu.mem[M_A]], dcpu.mem[0xffff & dcpu.mem[M_B]], dcpu.mem[0xffff & dcpu.mem[M_C]],
                    dcpu.mem[0xffff & dcpu.mem[M_X]], dcpu.mem[0xffff & dcpu.mem[M_Y]], dcpu.mem[0xffff & dcpu.mem[M_Z]],
                    dcpu.mem[0xffff & dcpu.mem[M_I]], dcpu.mem[0xffff & dcpu.mem[M_J]],
                    dcpu.mem[0xffff & dcpu.mem[M_SP]], dcpu.mem[0xffff & dcpu.mem[M_O]]);
        if (printStack > 0) {
            int sp = dcpu.mem[M_SP] & 0xffff;
            out.printf("  S: ");
            for (int i = 0; i < printStack; i++) {
                out.printf(" %04x", dcpu.mem[sp]);
                sp = (sp + 0x10000 - 1) % 0x10000;
            }
            out.println();
        }
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
}
