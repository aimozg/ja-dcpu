package dcpu;

import java.io.*;
import java.util.*;

/**
 * Notch's DCPU-16(tm)(c)(R)(ftw) specs v1.1 implementation.
 * <p/>
 * <p/>
 */
public final class Dcpu {
    ////////////////
    // NOTES
    ////////////////
    // * Registers are mapped to memory after addressable space for convenience (so operations take something from one
    //   mem cell and put something into another)
    // * NW is commonly used for 'Next Word in ram', NBI - non-basic-instruction
    // * Peripherals can be attached to monitor CPU ticks and read/writes to 4096-word memory lines (determined by highest nibble)

    ////////////////
    /// CONSTANTS
    ////////////////

    public enum Reg {
        A("A", 0, A_A), B("B", 1, A_B), C("C", 2, A_C),
        X("X", 3, A_X), Y("Y", 4, A_Y), Z("Z", 5, A_Z),
        I("I", 6, A_I), J("J", 7, A_J),
        PC("PC", 8, A_PC), SP("SP", 9, A_SP), EX("EX", 10, A_EX), IA("IA", 11, -1);

        public static final int BASE_ADDRESS = 0x10000;

        public final String name;
        public final int offset;
        public final int address;
        public final int acode;///< value code when referencing as a value, like SET A, SP. -1 for unaddressable regs

        // reverse map for looking up the Enum from the code, and convenience function
        private static final Map<Integer, Reg> OFFSET_LOOKUP = new HashMap<Integer, Reg>();
        private static final Map<String, Reg> NAME_LOOKUP = new HashMap<String, Reg>();

        public static Reg l(int offset) {
            return Reg.OFFSET_LOOKUP.get(offset);
        }

        public static Reg byName(String name) {
            return NAME_LOOKUP.get(name);
        }

        static {
            for (Reg r : Reg.values()) {
                OFFSET_LOOKUP.put(r.offset, r);
                NAME_LOOKUP.put(r.name, r);
            }
        }

        Reg(String name, int offset, int acode) {
            this.name = name;
            this.offset = offset;
            this.acode = acode;
            this.address = BASE_ADDRESS + offset;
        }
    }

    public enum BasicOp {
        SET("SET", 0x01, 1), ADD("ADD", 0x02, 2), SUB("SUB", 0x03, 2),
        MUL("MUL", 0x04, 2), MLI("MLI", 0x05, 2), DIV("DIV", 0x06, 3), DVI("DVI", 0x07, 3),
        MOD("MOD", 0x08, 3), AND("AND", 0x09, 1), BOR("BOR", 0x0a, 1), XOR("XOR", 0x0b, 1),
        SHR("SHR", 0x0c, 2), ASR("ASR", 0x0d, 2), SHL("SHL", 0x0e, 2),
        IFB("IFB", 0x10, 2), IFC("IFC", 0x11, 2), IFE("IFE", 0x12, 2), IFN("IFN", 0x13, 2),
        IFG("IFG", 0x14, 2), IFA("IFA", 0x15, 2), IFL("IFL", 0x16, 2), IFU("IFU", 0x17, 2);

        private static final Map<Integer, BasicOp> CODE_LOOKUP = new HashMap<Integer, BasicOp>();
        private static final Map<String, BasicOp> NAME_LOOKUP = new HashMap<String, BasicOp>();

        static {
            for (BasicOp op : values()) {
                CODE_LOOKUP.put(op.code, op);
                NAME_LOOKUP.put(op.name, op);
            }
        }

        public final String name;
        public final int code;
        public final int cycles;

        public static BasicOp l(int code) {
            return CODE_LOOKUP.get(code);
        }

        BasicOp(String name, int code, int cycles) {
            this.name = name;
            this.code = code;
            this.cycles = cycles;
        }

        public static BasicOp byName(String name) {
            return NAME_LOOKUP.get(name);
        }
    }

    public enum SpecialOp {
        JSR("JSR", 0x01, 3),
        INT("INT", 0x08, 4), ING("ING", 0x09, 1), INS("INS", 0x0a, 1),
        HWN("HWN", 0x10, 2), HWQ("HWQ", 0x11, 4), HWI("HWI", 0x12, 4);

        public final String name;
        public final int code;
        public final int cycles;

        private static final Map<Integer, SpecialOp> CODE_LOOKUP = new HashMap<Integer, SpecialOp>();
        private static final Map<String, SpecialOp> NAME_LOOKUP = new HashMap<String, SpecialOp>();

        public static SpecialOp l(int code) {
            return CODE_LOOKUP.get(code);
        }

        public static SpecialOp byName(String name) {
            return NAME_LOOKUP.get(name);
        }

        static {
            for (SpecialOp op : values()) {
                CODE_LOOKUP.put(op.code, op);
                NAME_LOOKUP.put(op.name, op);
            }
        }

        SpecialOp(String name, int code, int cycles) {
            this.name = name;
            this.code = code;
            this.cycles = cycles;
        }
    }

    //////
    // Opcode constants
    public static final int O_NBI = 0x00;
    public static final int O_SET = BasicOp.SET.code;
    public static final int O_ADD = BasicOp.ADD.code;
    public static final int O_SUB = BasicOp.SUB.code;
    public static final int O_MUL = BasicOp.MUL.code;
    public static final int O_MLI = BasicOp.MLI.code;
    public static final int O_DIV = BasicOp.DIV.code;
    public static final int O_DVI = BasicOp.DVI.code;
    public static final int O_MOD = BasicOp.MOD.code;
    public static final int O_AND = BasicOp.AND.code;
    public static final int O_BOR = BasicOp.BOR.code;
    public static final int O_XOR = BasicOp.XOR.code;
    public static final int O_SHR = BasicOp.SHR.code;
    public static final int O_ASR = BasicOp.ASR.code;
    public static final int O_SHL = BasicOp.SHL.code;
    // 0x0f reserved
    public static final int O_IFB = BasicOp.IFB.code;
    public static final int O_IFC = BasicOp.IFC.code;
    public static final int O_IFE = BasicOp.IFE.code;
    public static final int O_IFN = BasicOp.IFN.code;
    public static final int O_IFG = BasicOp.IFG.code;
    public static final int O_IFA = BasicOp.IFA.code;
    public static final int O_IFL = BasicOp.IFL.code;
    public static final int O_IFU = BasicOp.IFU.code;
    // 0x18-0x1f reserved
    // Special opcodes
    public static final int O__JSR = SpecialOp.JSR.code;
    public static final int O__INT = SpecialOp.INT.code;
    public static final int O__ING = SpecialOp.ING.code;
    public static final int O__INS = SpecialOp.INS.code;
    public static final int O__HWN = SpecialOp.HWN.code;
    public static final int O__HWQ = SpecialOp.HWQ.code;
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
    public static final int REGS_COUNT = Reg.values().length; ///< Number of registers

    // Command parts (opcode, A, B)
    public static final int C_O_BITLEN = 5;
    public static final int C_B_BITLEN = 5;
    public static final int C_A_BITLEN = 6;
    public static final int C_B_SHIFT = C_O_BITLEN;
    public static final int C_A_SHIFT = C_O_BITLEN + C_B_BITLEN;
    // to get a strip of N binary 1s, we do 1 << (N+1) and substract 1
    public static final int C_O_MASK = (1 << C_O_BITLEN) * 2 - 1;
    public static final int C_B_MASK = ((1 << C_B_BITLEN) * 2 - 1) << C_B_SHIFT;
    public static final int C_A_MASK = ((1 << C_A_BITLEN) * 2 - 1) << C_A_SHIFT;
    public static final int C_NBI_O_BITLEN = 5;
    public static final int C_NBI_A_BITLEN = 6;
    public static final int C_NBI_A_MASK = C_B_MASK;
    public static final int C_NBI_A_SHIFT = C_B_SHIFT;
    public static final int C_NBI_O_MASK = C_A_MASK;
    public static final int C_NBI_O_SHIFT = C_A_SHIFT;
    // Command value types (take one and shift with C_x_SHIFT)
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
    //   [Register]
    public static final int A_M_REG = 8; // or with REG_x
    public static final int A_M_A = A_M_REG | Reg.A.offset;
    public static final int A_M_B = A_M_REG | Reg.B.offset;
    public static final int A_M_C = A_M_REG | Reg.C.offset;
    public static final int A_M_X = A_M_REG | Reg.X.offset;
    public static final int A_M_Y = A_M_REG | Reg.Y.offset;
    public static final int A_M_Z = A_M_REG | Reg.Z.offset;
    public static final int A_M_I = A_M_REG | Reg.I.offset;
    public static final int A_M_J = A_M_REG | Reg.J.offset;
    //  [Register+NW]
    public static final int A_M_NW_REG = 0x10; // or with REG_x
    public static final int A_M_NW_A = A_M_NW_REG | Reg.A.offset;
    public static final int A_M_NW_B = A_M_NW_REG | Reg.B.offset;
    public static final int A_M_NW_C = A_M_NW_REG | Reg.C.offset;
    public static final int A_M_NW_X = A_M_NW_REG | Reg.X.offset;
    public static final int A_M_NW_Y = A_M_NW_REG | Reg.Y.offset;
    public static final int A_M_NW_Z = A_M_NW_REG | Reg.Z.offset;
    public static final int A_M_NW_I = A_M_NW_REG | Reg.I.offset;
    public static final int A_M_NW_J = A_M_NW_REG | Reg.J.offset;
    //   Special registers and stack
    public static final int A_PUSHPOP = 0x18;
    public static final int A_PEEK = 0x19;
    public static final int A_PICK = 0x1a;
    public static final int A_SP = 0x1b;
    public static final int A_PC = 0x1c;
    public static final int A_EX = 0x1d;
    public static final int A_M_NW = 0x1e; // [NW]
    public static final int A_NW = 0x1f; // NW
    //  Constant values
    public static final int A_CONST = 0x20; // + with const
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
    // Additional instruction length from operand (1 if has NW, 0 otherwise)
    public static final int[] OPERAND_LENGTH = {
            // Register
            0, 0, 0, 0, 0, 0, 0, 0,
            // [Register]
            0, 0, 0, 0, 0, 0, 0, 0,
            // [NW+register]
            1, 1, 1, 1, 1, 1, 1, 1,
            // PUSHPOP PEEK PICK SP PC EX [NW] NW
            0, 0, 1, 0, 0, 0, 1, 1,
            // literal
            0, 0, 0, 0, 0, 0, 0, 0
    };
    // True if instruction accesses memory (false for literals and registers)
    public static final boolean[] OPERAND_MEMACCESS = {
            // Register
            false, false, false, false, false, false, false, false,
            // [Register]
            true, true, true, true, true, true, true, true,
            // [NW+register]
            true, true, true, true, true, true, true, true,
            // PUSHPOP PEEK PICK SP PC EX [NW] NW
            true, true, true, false, false, false, true, true,
            // literal
            false, false, false, false, false, false, false, false
    };

    public static final int RAM_SIZE = 0x10000;
    public static final long CPU_FREQUENCY = 100 * 1000;
    public static final long NANO_TICK = 1000 * 1000 * 1000 / CPU_FREQUENCY;
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
    public static final int M_EX = Reg.EX.address;
    public static final int M_CV = Reg.BASE_ADDRESS + REGS_COUNT; // constant value
    // Memory cell names
    public static final String[] MEM_NAMES = {
            Reg.A.name, Reg.B.name, Reg.C.name, Reg.X.name, Reg.Y.name, Reg.Z.name, Reg.I.name, Reg.J.name,
            Reg.PC.name, Reg.SP.name, Reg.EX.name,
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "10", "11", "12", "13", "14", "15",
            "16", "17", "18", "19", "20", "21", "22", "23",
            "24", "25", "26", "27", "28", "29", "30", "31"
    };

    ///////////////////////////////////////////////////////////////
    // CORE CPU FUNCTIONS
    ///////////////////////////////////////////////////////////////

    // Memory cells: 64k RAM + 8 general-purpose regs + SP + PC + EX + 32 constants
    public final short[] mem = new short[M_CV + 32];
    public boolean reserved = false; // true if reserved operation executed
    public volatile boolean halt = false;// halt execution

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
     * <p/>
     * TODO delay
     */
    public void step(boolean skip) {
        short ppc = mem[M_PC];
        short psp = mem[M_SP];
        if (!skip && stepListener != null) stepListener.preExecute(mem[M_PC]);

        int cmd = mem[(mem[M_PC]++) & 0xffff] & 0xffff; // command value
        int opcode = cmd & C_O_MASK;
        // a,b: raw codes, addresses, values, signed values
        // in NBI: b stores NBO
        int a, b, aa, ba, av, bv, asv, bsv;
        if (opcode != O_NBI) {
            a = (cmd & C_A_MASK) >> C_A_SHIFT;
            b = (cmd & C_B_MASK) >> C_B_SHIFT;
            aa = getaddr(a, true) & 0x1ffff;
            ba = getaddr(b, false) & 0x1ffff;
            if (skip) {
                mem[M_SP] = psp;
                return;
            }
            asv = memget(aa);
            bsv = memget(ba);
            av = asv & 0xffff;
            bv = bsv & 0xffff;
        } else {
            a = (cmd & C_NBI_A_MASK) >> C_NBI_A_SHIFT;
            b = (cmd & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
            aa = getaddr(a, true) & 0x1ffff;
            if (skip) {
                mem[M_SP] = psp;
                return;
            }
            ba = 0;
            asv = memget(aa);
            av = asv & 0xffff;
            bv = bsv = 0;
        }

        // debug
        //_dstep(skip, opcode, aa, ba, av, bv);

        boolean printedBranch = false;
        int rslt = mem[ba]; // new 'b' value
        int exreg = mem[M_EX]; // new 'EX' value
        BasicOp bop = BasicOp.l(opcode);
        if (bop != null) {
            switch (bop) {
                case SET:
                    rslt = av;
                    break;
                case ADD:
                    rslt = bv + av;
                    exreg = (rslt > 0xffff) ? 1 : 0;
                    break;
                case SUB:
                    rslt = bv - av;
                    exreg = (rslt < 0) ? 0xffff : 0;
                    break;
                case MUL:
                    rslt = bv * av;
                    exreg = rslt >> 16;
                    break;
                case MLI:
                    rslt = bsv * asv;
                    exreg = rslt >> 16;
                    break;
                case DIV:
                    if (av == 0) {
                        exreg = 0;
                        rslt = 0;
                    } else {
                        rslt = (bv / av);
                        exreg = ((bv << 16) / av);
                    }
                    break;
                case DVI:
                    if (asv == 0) {
                        exreg = 0;
                        rslt = 0;
                    } else {
                        rslt = (bsv / asv);
                        exreg = ((bsv << 16) / asv);
                    }
                    break;
                case MOD:
                    if (av == 0) {
                        rslt = 0;
                    } else {
                        rslt = (short) (bv % av);
                    }
                    break;
                case SHL:
                    rslt = bv << av;
                    exreg = (bv << av) >> 16;
                    break;
                case SHR:
                    rslt = av >>> bv;
                    exreg = (bv << 16) >>> av;
                    break;
                case ASR:
                    rslt = av >> bv;
                    exreg = (bv << 16) >> av;
                    break;
                case AND:
                    rslt = av & bv;
                    break;
                case BOR:
                    rslt = av | bv;
                    break;
                case XOR:
                    rslt = av ^ bv;
                    break;
                case IFE:
                    if (av != bv) {
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                case IFN:
                    if (av == bv) {
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                case IFG:
                    if (!(bv > av)) {
                        // TODO DRY : move this block after switch
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                case IFB:
                    if ((av & bv) == 0) {
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                case IFC:
                    if ((av & bv) != 0) {
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                case IFA:
                    if (!(bsv > asv)) {
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                case IFL:
                    if (!(bv < av)) {
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                case IFU:
                    if (!(bsv < asv)) {
                        printedBranch = true;
                        if (!skip && stepListener != null) stepListener.postExecute(ppc);
                        step(true);
                    }
                    break;
                default:
                    throw new RuntimeException("DCPU Opcode not implemented: " + bop);
            }
        } else {
            if (opcode == O_NBI) {
                SpecialOp sop = SpecialOp.l(b);
                if (sop == null) {
                    reserved = true;
                    halt = true;
                } else {
                    switch (sop) {
                        case JSR:
                            mem[(--mem[M_SP]) & 0xffff] = mem[M_PC];
                            mem[M_PC] = (short) av;
                            break;
                        case INT:
                            // TODO INT
                            break;
                        case ING:
                            // TODO ING
                            break;
                        case INS:
                            // TODO INS
                            break;
                        case HWN:
                            // TODO HWN
                            break;
                        case HWQ:
                            // TODO HWQ
                            break;
                        case HWI:
                            // TODO HWI
                            break;
                    }
                }
            } else {
                // invalid opcode
                reserved = true;
                halt = true;
            }
        }
        // overwrite 'a' unless it is constant
        if (aa < M_CV && OPCODE_MODMEM[opcode]) memset(aa, (short) rslt);
        mem[M_EX] = (short) exreg;
        for (Peripheral peripheral : peripherals) {
            peripheral.tick(cmd);
        }
        if (!printedBranch && !skip && stepListener != null) stepListener.postExecute(ppc);
    }

    /**
     * Sets memory[addr] to value, calling peripheral hook, if installed
     * <p/>
     * TODO add "debugger" parameter, which also passed to peripheral,
     * so its state is not changed (e.g. key buffer not erased when debugger views memory)
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

    /**
     * Zeroizes memory
     */
    public void memzero() {
        Arrays.fill(mem, 0, RAM_SIZE, (short) 0);
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
        return String.format("R A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x  PC=%04x SP=%04x EX=%04x",
                mem[M_A], mem[M_B], mem[M_C], mem[M_X], mem[M_Y], mem[M_Z], mem[M_I], mem[M_J],
                mem[M_PC], mem[M_SP], mem[M_EX]);
    }

    public String _dmem(int addr) {
        return (addr < M_A) ? String.format("(%04x)", addr) : MEM_NAMES[addr - M_A];
    }

    public String _dvalmem(int addr) {
        return (addr < M_A) ? String.format("(%04x) : %04x (%s)", addr, mem[addr], Character.toString((char) mem[addr])) : MEM_NAMES[addr - M_A];
    }


    /**
     * Returns memory address for operand code. 0 returns address of register A and so on.
     * May modify values of PC (in case of "next word of ram") and SP (when PUSH, POP)
     *
     * @param isa true when evaluating "a"
     */
    private int getaddr(int cmd, boolean isa) {
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
            return M_CV + cmd - 0x20 - 1;
        } else switch (cmd) {
            case A_PUSHPOP:
                // isa?POP:PUSH
                return (isa ? (mem[M_SP]++) : (--mem[M_SP])) & 0xffff;
            case A_PEEK:
                return mem[M_SP] & 0xffff;
            case A_PICK:
                return (M_SP + mem[M_PC]++) & 0xffff;
            case A_SP:
                return M_SP;
            case A_PC:
                return M_PC;
            case A_EX:
                return M_EX;
            case A_M_NW:
                return mem[mem[M_PC]++] & 0xffff;
            case A_NW:
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
        if (srcoff < 0 || len < 0 || srcoff + len >= RAM_SIZE)
            throw new IllegalArgumentException("Bad offset/length");
        System.arraycopy(buffer, srcoff, mem, dstoff, len);
    }

    public void upload(short[] buffer) {
        upload(buffer, 0, buffer.length, 0);
    }

    public void upload(File infile) throws IOException {
        upload(new FileInputStream(infile));
    }

    public void upload(InputStream stream) throws IOException {
        BufferedInputStream instream = new BufferedInputStream(stream);
        int len = instream.available();
        if (len % 2 == 1) throw new IOException(String.format("Odd file size (0x%x)\n", len));
        len /= 2;
        if (len > 0x10000) throw new IOException(String.format("Too large file (0x%x)\n", len));
        short[] bytecode = new short[len];
        for (int i = 0; i < len; i++) {
            int lo = instream.read();
            int hi = instream.read();
            if (lo == -1 || hi == -1) throw new IOException(String.format("Failed to read data from file\n"));
            bytecode[i] = (short) ((hi << 8) | lo);
        }
        upload(bytecode);

    }

    public int pc() {
        return mem[M_PC] & 0xffff;
    }

    public int sp() {
        return mem[M_SP] & 0xffff;
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
