package dcpu;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Notch's DCPU-16(tm)(c)(R)(ftw) specs v1.1 implementation.
 * <p/>
 * Created by IntelliJ IDEA.
 * Author: aimozg
 * Date: 28.03.12
 * Time: 19:15
 */
public class Dcpu {

    ////////////////
    // NOTES
    ////////////////
    // * Registers are mapped to memory after addressable space for convenience (so operations take something from one
    //   mem cell and put something into another)
    // * Some magic about PPC, PSP, and getaddr explained in getaddr comment
    // * NW is commonly used for 'Next Word in ram', NBI - non-basic-instruction
    // * Peripherals can be attached to monitor CPU ticks and read/writes to 4096-word memory lines (determined by highest nibble)

    ////////////////
    /// CONSTANTS
    ////////////////

    public enum Reg {
        A("A", 0), B("B", 1), C("C", 2),
        X("X", 3), Y("Y", 4), Z("Z", 5),
        I("I", 6), J("J", 7),
        PC("PC", 8), SP("SP", 9), O("O", 10),
        PPC("PPC", 11), PSP("PSP", 12);
        
        public static final int BASE_ADDRESS = 0x10000;
        
        public final String name;
        public final int offset;
        public final int address;
        
        // reverse map for looking up the Enum from the code, and convenience function
        private static final Map<Integer, Reg> LOOKUP = new HashMap<Integer, Reg>();
        public static Reg l(int offset) { return Reg.LOOKUP.get(offset); }

        static {
            for(Reg r : EnumSet.allOf(Reg.class)) {
                 LOOKUP.put(r.offset, r);
            }
        }

        Reg(String name, int offset) {
            this.name = name;
            this.offset = offset;
            this.address = BASE_ADDRESS + offset; 
        }
    }

    //////
    // Opcode constants
    public static final int O_NBI = 0;
    public static final int O_SET = 1;
    public static final int O_ADD = 2;
    public static final int O_SUB = 3;
    public static final int O_MUL = 4;
    public static final int O_DIV = 5;
    public static final int O_MOD = 6;
    public static final int O_SHL = 7;
    public static final int O_SHR = 8;
    public static final int O_AND = 9;
    public static final int O_BOR = 10;
    public static final int O_XOR = 11;
    public static final int O_IFE = 12;
    public static final int O_IFN = 13;
    public static final int O_IFG = 14;
    public static final int O_IFB = 15;
    public static final int O__RESVD = 0; // reserved
    public static final int O__JSR = 1;//NBI
    public static final String[] OPCODE_NAMES = {
            "?", "SET", "ADD", "SUB",
            "MUL", "DIV", "MOD", "SHL",
            "SHR", "AND", "BOR", "XOR",
            "IFE", "IFN", "IFG", "IFB"
    };
    public static final String[] OPCODE0_NAMES = {
            "?", "JSR", "?", "?", "?", "?", "?", "?", //0x00-0x07
            "?", "?", "?", "?", "?", "?", "?", "?",//0x08-0x0f
            "?", "?", "?", "?", "?", "?", "?", "?",//0x10-0x17
            "?", "?", "?", "?", "?", "?", "?", "?",//0x18-0x1f
            "?", "?", "?", "?", "?", "?", "?", "?",//0x20-0x27
            "?", "?", "?", "?", "?", "?", "?", "?",//0x28-0x2f
            "?", "?", "?", "?", "?", "?", "?", "?",//0x30-0x37
            "?", "?", "?", "?", "?", "?", "?", "?"//0x38-0x3f
    };
    public static final boolean[] OPCODE0_RESERVED = {
            true, false, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true
    };
    // operations that place their result into memory cell
    public static final boolean[] OPCODE_MODMEM = {
            false, true, true, true,
            true, true, true, true,
            true, true, true, true,
            false, false, false, false
    };
    public static final boolean[] OPCODE0_MODMEM = {
            false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,
            false, false, false, false, false, false, false
    };
    // Register constants
    public static final int REGS_COUNT = 8 + 1 + 1 + 1; ///< Register count: 8 GP, PC, SP, O

    // Command parts (opcode, A, B)
    public static final int C_O_MASK = 0x000F;
    public static final int C_A_MASK = 0x03F0;
    public static final int C_B_MASK = 0xFC00;
    public static final int C_A_SHIFT = 4;
    public static final int C_B_SHIFT = 10;
    public static final int C_NBI_A_MASK = C_B_MASK;
    public static final int C_NBI_A_SHIFT = C_B_SHIFT;
    public static final int C_NBI_O_MASK = C_A_MASK;
    public static final int C_NBI_O_SHIFT = C_A_SHIFT;
    // Command address types (take one and shift with C_x_SHIFT)
    //   Plain register
    public static final int A_REG = 0;// | with REG_x
    public static final int A_A = A_REG | Reg.A.offset;
    public static final int A_B = A_REG | Reg.B.offset;
    public static final int A_C = A_REG | Reg.C.offset;
    public static final int A_X = A_REG | Reg.X.offset;
    public static final int A_Y = A_REG | Reg.Y.offset;
    public static final int A_Z = A_REG | Reg.Z.offset;
    public static final int A_I = A_REG | Reg.I.offset;
    public static final int A_J = A_REG | Reg.J.offset;
    //   (Register)
    public static final int A_M_REG = 8; // or with REG_x
    public static final int A_M_A = A_M_REG | Reg.A.offset;
    public static final int A_M_B = A_M_REG | Reg.B.offset;
    public static final int A_M_C = A_M_REG | Reg.C.offset;
    public static final int A_M_X = A_M_REG | Reg.X.offset;
    public static final int A_M_Y = A_M_REG | Reg.Y.offset;
    public static final int A_M_Z = A_M_REG | Reg.Z.offset;
    public static final int A_M_I = A_M_REG | Reg.I.offset;
    public static final int A_M_J = A_M_REG | Reg.J.offset;
    //  (Register+NW)
    public static final int A_M_NW_REG = 16; // or with REG_x
    public static final int A_M_NW_A = A_M_NW_REG | Reg.A.offset;
    public static final int A_M_NW_B = A_M_NW_REG | Reg.B.offset;
    public static final int A_M_NW_C = A_M_NW_REG | Reg.C.offset;
    public static final int A_M_NW_X = A_M_NW_REG | Reg.X.offset;
    public static final int A_M_NW_Y = A_M_NW_REG | Reg.Y.offset;
    public static final int A_M_NW_Z = A_M_NW_REG | Reg.Z.offset;
    public static final int A_M_NW_I = A_M_NW_REG | Reg.I.offset;
    public static final int A_M_NW_J = A_M_NW_REG | Reg.J.offset;
    //   Special registers and stack
    public static final int A_POP = 24;
    public static final int A_PEEK = 25;
    public static final int A_PUSH = 26;
    public static final int A_SP = 27;
    public static final int A_PC = 28;
    public static final int A_O = 29;
    public static final int A_M_NW = 30; // (NW)
    public static final int A_NW = 31; // NW
    //  Constant values
    public static final int A_CONST = 32; // + with const
    public static final int A_0 = A_CONST + 0;
    public static final int A_1 = A_CONST + 1;
    public static final int A_2 = A_CONST + 2;
    public static final int A_3 = A_CONST + 3;
    public static final int A_4 = A_CONST + 4;
    public static final int A_5 = A_CONST + 5;
    public static final int A_6 = A_CONST + 6;
    public static final int A_7 = A_CONST + 7;
    public static final int A_8 = A_CONST + 8;
    public static final int A_9 = A_CONST + 9;
    public static final int A_10 = A_CONST + 10;
    public static final int A_11 = A_CONST + 11;
    public static final int A_12 = A_CONST + 12;
    public static final int A_13 = A_CONST + 13;
    public static final int A_14 = A_CONST + 14;
    public static final int A_15 = A_CONST + 15;
    public static final int A_16 = A_CONST + 16;
    public static final int A_17 = A_CONST + 17;
    public static final int A_18 = A_CONST + 18;
    public static final int A_19 = A_CONST + 19;
    public static final int A_20 = A_CONST + 20;
    public static final int A_21 = A_CONST + 21;
    public static final int A_22 = A_CONST + 22;
    public static final int A_23 = A_CONST + 23;
    public static final int A_24 = A_CONST + 24;
    public static final int A_25 = A_CONST + 25;
    public static final int A_26 = A_CONST + 26;
    public static final int A_27 = A_CONST + 27;
    public static final int A_28 = A_CONST + 28;
    public static final int A_29 = A_CONST + 29;
    public static final int A_30 = A_CONST + 30;
    public static final int A_31 = A_CONST + 31;

    //////
    // Register addresses
    public static final int M_A = Reg.BASE_ADDRESS;
    public static final int M_B = Reg.B.address;
    public static final int M_C = Reg.C.address;
    public static final int M_X = Reg.X.address;
    public static final int M_Y = Reg.Y.address;
    public static final int M_Z = Reg.Z.address;
    public static final int M_I = Reg.I.address;
    public static final int M_J = Reg.J.address;
    public static final int M_PC = Reg.PC.address;
    public static final int M_SP = Reg.SP.address;
    public static final int M_O = Reg.O.address;
    public static final int M_PPC = Reg.PPC.address; // prev PC (PC before execution)
    public static final int M_PSP = Reg.PSP.address; // prev SP (SP before execution)
    public static final int M_CV = Reg.PSP.address + 1; // constant value
    // Memory cell names
    public static final String[] MEM_NAMES = {
            Reg.A.name, Reg.B.name, Reg.C.name, Reg.X.name, Reg.Y.name, Reg.Z.name, Reg.I.name, Reg.J.name,
            Reg.PC.name, Reg.SP.name, Reg.O.name, Reg.PPC.name, Reg.PSP.name,
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "10", "11", "12", "13", "14", "15",
            "16", "17", "18", "19", "20", "21", "22", "23",
            "24", "25", "26", "27", "28", "29", "30", "31"
    };
    
    ///////////////////////////////////////////////////////////////
    // CORE CPU FUNCTIONS
    ///////////////////////////////////////////////////////////////

    // Memory cells: 64k RAM + 8 general-purpose regs + SP + PC + O + PPC + PSP + 32 constants
    public final short[] mem = new short[M_CV + 32];
    public boolean reserved = false; // true if reserved operation executed
    public boolean halt = false;// halt execution

    /**
     * Runs until hitting Opcode 0
     */
    public void run() {
        halt = false;
        while (!halt) {
            step(false);
        }
    }

    /**
     * Executes specified number of steps
     */
    public void run(int nsteps) {
        while (nsteps-- > 0) {
            step(false);
        }
    }

    public Listener<Short> stepListener;

    /**
     * Execute one operation (skip = false) or skip one operation.
     */
    public void step(boolean skip) {
        // save prev PC and prev SP
        mem[M_PPC] = mem[M_PC];
        mem[M_PSP] = mem[M_SP];

        int cmd = mem[(mem[M_PC]++) & 0xffff] & 0xffff; // command value
        int opcode = cmd & C_O_MASK;
        // a,b: raw codes, addresses, values
        // in NBI: b stores NBO
        int a, b, aa, ba, av, bv;
        if (opcode != O_NBI) {
            a = (cmd & C_A_MASK) >> C_A_SHIFT;
            b = (cmd & C_B_MASK) >> C_B_SHIFT;
            aa = getaddr(a, OPCODE_MODMEM[opcode]) & 0x1ffff;
            ba = getaddr(b, false) & 0x1ffff;
            if (skip) {
                mem[M_SP] = mem[M_PSP];
                return;
            }
            av = memget(aa) & 0xffff;
            bv = memget(ba) & 0xffff;
        } else {
            a = (cmd & C_NBI_A_MASK) >> C_NBI_A_SHIFT;
            b = (cmd & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
            aa = getaddr(a, OPCODE0_MODMEM[b]) & 0x1ffff;
            if (skip) {
                mem[M_SP] = mem[M_PSP];
                return;
            }
            ba = 0;
            av = memget(aa) & 0xffff;
            bv = 0;
        }

        // debug
        //_dstep(skip, opcode, aa, ba, av, bv);

        int rslt = mem[aa]; // new 'a' value
        int oreg = mem[M_O]; // new 'O' value
        switch (opcode) {
            case O_NBI:
                switch (b) {
                    case O__JSR:
                        mem[(--mem[M_SP]) & 0xffff] = mem[M_PC];
                        mem[M_PC] = (short) av;
                        break;
                    default:
                        reserved = true;
                        halt = true;
                        break;
                }
                break;
            case O_SET:
                rslt = bv;
                break;
            case O_ADD:
                rslt = av + bv;
                oreg = (rslt > 0xffff) ? 1 : 0;
                break;
            case O_SUB:
                rslt = av - bv;
                oreg = (rslt < 0) ? 1 : 0;
                break;
            case O_MUL:
                rslt = av * bv;
                oreg = rslt >> 16;
                break;
            case O_DIV:
                if (bv == 0) {
                    oreg = 0;
                    rslt = 0;
                } else {
                    rslt = (short) (av / bv);
                    oreg = (short) ((av << 16) / bv);
                }
                break;
            case O_MOD:
                if (bv == 0) {
                    rslt = 0;
                } else {
                    rslt = (short) (av % bv);
                }
                break;
            case O_SHL:
                rslt = av << bv;
                oreg = rslt >> 16;
                break;
            case O_SHR:
                rslt = av >> bv;
                oreg = av - (rslt << bv);
                break;
            case O_AND:
                rslt = av & bv;
                break;
            case O_BOR:
                rslt = av | bv;
                break;
            case O_XOR:
                rslt = av ^ bv;
                break;
            case O_IFE:
                if (av != bv) step(true);
                break;
            case O_IFN:
                if (av == bv) step(true);
                break;
            case O_IFG:
                if (av <= bv) step(true);
                break;
            case O_IFB:
                if ((av & bv) == 0) step(true);
                break;
        }
        // overwrite 'a' unless it is constant
        if (aa < M_CV && OPCODE_MODMEM[opcode]) memset(aa, (short) rslt);
        mem[M_O] = (short) oreg;
        for (Peripheral peripheral : peripherals) {
            peripheral.tick(cmd);
        }
        if (!skip && stepListener != null) stepListener.event(mem[M_PC]);
    }

    /**
     * Sets memory[addr] to value, calling peripheral hook, if installed
     */
    public void memset(int addr, short value) {
        short oldval = mem[addr];
        int line = addr >>> 12;
        mem[addr] = value;
        if (line < memlines.length && memlines[line] != null) {
            memlines[line].onMemset(addr & 0x0fff, value, oldval);
        }
    }

    public short memget(int addr) {
        int line = addr >>> 12;
        if (line < memlines.length && memlines[line] != null) {
            return memlines[line].onMemget(addr & 0x0fff);
        }
        return mem[addr];
    }


    public void reset() {
        reserved = false;
        halt = false;
        for (int i = 0; i < M_CV - M_A; i++) mem[M_A + i] = 0;
        for (int i = 0; i < 32; i++) {
            mem[M_CV + i] = (short) i;
        }
    }

    public Dcpu() {
        reset();
    }

    ////////////////////////////////////////////
    // UTILITY, DEBUG, AND INTERNAL FUNCTIONS

    /**
     * Generates command code for specified opcode, 'a', and 'b'.
     * <p/>
     * Example: gencmd(O_SET, A_PC, A_NW) for "set PC, next_word_of_ram"
     */
    public static short gencmd(int opcode, int a, int b) {
        return (short) (opcode | a << C_A_SHIFT | b << C_B_SHIFT);
    }

    /**
     * Generates command code for non-basic instruction
     */
    public static short gencmd_nbi(int opcode, int a) {
        return (short) (opcode << C_NBI_O_SHIFT | a << C_NBI_A_SHIFT);
    }

    // debug
    private void _d(String ln, Object... args) {
        System.out.printf(ln, args);
    }

    /**
     * List of all registers with their hex values
     */
    public String _dregs() {
        return String.format("R A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x  PC=%04x SP=%04x O=%04x",
                mem[M_A], mem[M_B], mem[M_C], mem[M_X], mem[M_Y], mem[M_Z], mem[M_I], mem[M_J],
                mem[M_PC], mem[M_SP], mem[M_O]);
    }

    private String _dmem(int addr) {
        return (addr < M_A) ? String.format("(%04x)", addr) : MEM_NAMES[addr - M_A];
    }

    @SuppressWarnings("unused")
    private void _dstep(boolean skip, int opcode, int aa, int ba, int av, int bv) {
        _d("%s%s %s=%04x %s=%04x\n", skip ? "; " : "> ", OPCODE_NAMES[opcode], _dmem(aa), av, _dmem(ba), bv);
    }

    /**
     * Returns memory address for operand code. 0 returns address of register A and so on.
     * May modify values of PC (in case of "next word of ram") and SP (when PUSH, POP)
     * <p/>
     * Ok, so here is the black magic:
     * SET [PC+20], PC
     * when parsing [PC+20] it modifies PC, but when parsing PC, it should return previous, unmodified value.
     * that's what "write" is for.
     */
    private int getaddr(int cmd, boolean write) {
        if (cmd <= 0x07) {
            // register
            return M_A + cmd;
        } else if (cmd <= 0x0f) {
            // [register]
            return mem[M_A + cmd - 8] & 0xffff;
        } else if (cmd <= 0x17) {
            // [next word + register]
            return (mem[M_A + cmd - 16] + mem[mem[M_PC]++]) & 0xffff;
        } else if (cmd >= 0x20 && cmd <= 0x3f) {
            // literal value
            return M_CV + cmd - 0x20;
        } else switch (cmd) {
            case 0x18:
                // POP
                return (mem[M_SP]++) & 0xffff;
            case 0x19:
                // PEEK
                return mem[M_SP] & 0xffff;
            case 0x1a:
                // PUSH
                return (--mem[M_SP]) & 0xffff;
            case 0x1b:
                // SP
                return write ? M_SP : M_PSP;
            case 0x1c:
                // PC
                return write ? M_PC : M_PPC;
            case 0x1d:
                // O
                return M_O;
            case 0x1e:
                // [next word]
                return mem[mem[M_PC]++] & 0xffff;
            case 0x1f:
                // next word (literal)
                return mem[M_PC]++ & 0xffff;
            default:
                throw new RuntimeException("Unknown cmd value: " + cmd);
        }
    }

    public short getreg(Reg reg) {
        return (short) (mem[reg.address] & 0xffff);
    }

    public void upload(short[] buffer, int srcoff, int len, int dstoff) {
        if (srcoff >= 0x10000 || srcoff < 0 || len < 0 || srcoff + len >= 0x10000)
            throw new IllegalArgumentException("Bad offset/length");
        System.arraycopy(buffer, srcoff, mem, dstoff, len);
    }

    public void upload(short[] buffer) {
        upload(buffer, 0, buffer.length, 0);
    }


    ///////////////////////////////////////////////////////////////////////////
    //// PERIPHERAL

    /**
     * Peripheral to DCPU.
     * <p/>
     * Communication
     */
    public abstract static class Peripheral {

        public Dcpu cpu;
        public int baseaddr;

        /**
         * This method is called every CPU cycle. cmd is last command code
         */
        public void tick(int cmd) {

        }

        /**
         * This method is called when program or other peripheral writes "newval" to
         * memory address "baseaddr"+"offset". Note that cpu.mem[baseaddr+offset] already contains newval
         */
        public void onMemset(int offset, short newval, short oldval) {

        }

        /**
         * This method is called when program or other peripheral reads value from
         * memory address "baseaddr"+"offset".
         */
        public short onMemget(int offset) {
            return cpu.mem[baseaddr + offset];
        }

        /**
         * Called when attached to cpu
         */
        public void attachedTo(Dcpu cpu, int baseaddr) {
            this.cpu = cpu;
            this.baseaddr = baseaddr;
        }

        /**
         * Called when detached from cpu
         */
        public void detached() {
            this.cpu = null;
        }
    }

    final Peripheral[] memlines = new Peripheral[16];
    final List<Peripheral> peripherals = new LinkedList<Peripheral>();

    public void attach(Peripheral peripheral, int line) {
        if (line != -1) {
            if (memlines[line] != null) {
                throw new IllegalStateException("Peripheral already attached to line");
            }
            memlines[line] = peripheral;
        }
        peripherals.add(peripheral);
        peripheral.attachedTo(this, line << 12);
    }

    public void detach(Peripheral peripheral) {
        if (peripheral.baseaddr != -1) {
            memlines[peripheral.baseaddr >> 12] = null;
        }
        peripherals.remove(peripheral);
        peripheral.detached();
    }
}
