package dcpu;

import java.io.*;
import java.util.*;

import dcpu.Dcpu.BasicOp;

/**
 * Notch's DCPU-16(tm)(c)(R)(ftw) specs v1.7 implementation.
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
        SET("SET", 0x01, 1, true), ADD("ADD", 0x02, 2, true), SUB("SUB", 0x03, 2, true),
        MUL("MUL", 0x04, 2, true), MLI("MLI", 0x05, 2, true), DIV("DIV", 0x06, 3, true), DVI("DVI", 0x07, 3, true),
        MOD("MOD", 0x08, 3, true), MDI("MDI", 0x09, 3, true), AND("AND", 0x0a, 1, true), BOR("BOR", 0x0b, 1, true), XOR("XOR", 0x0c, 1, true),
        SHR("SHR", 0x0d, 1, true), ASR("ASR", 0x0e, 1, true), SHL("SHL", 0x0f, 1, true),
        IFB("IFB", 0x10, 2, false), IFC("IFC", 0x11, 2, false), IFE("IFE", 0x12, 2, false), IFN("IFN", 0x13, 2, false),
        IFG("IFG", 0x14, 2, false), IFA("IFA", 0x15, 2, false), IFL("IFL", 0x16, 2, false), IFU("IFU", 0x17, 2, false),
        ADX("ADX", 0x1a, 3, true), SBX("SBX", 0x1b, 3, true),
        STI("STI", 0x1e, 2, true), STD("STD", 0x1f, 2, true);

        private static final Map<Integer, BasicOp> CODE_LOOKUP = new HashMap<Integer, BasicOp>();
        private static final Map<String, BasicOp> NAME_LOOKUP = new HashMap<String, BasicOp>();

        public static final Set<BasicOp> OPS_IF;

        static {
            for (BasicOp op : values()) {
                CODE_LOOKUP.put(op.code, op);
                NAME_LOOKUP.put(op.name, op);
            }
            EnumSet<BasicOp> _ops_if = EnumSet.noneOf(BasicOp.class);
            _ops_if.add(IFB);
            _ops_if.add(IFC);
            _ops_if.add(IFE);
            _ops_if.add(IFN);
            _ops_if.add(IFG);
            _ops_if.add(IFA);
            _ops_if.add(IFL);
            _ops_if.add(IFU);
            OPS_IF = Collections.unmodifiableSet(_ops_if);
        }

        public final String name;
        public final int code;
        public final int cycles;
        public final boolean modb;///< true if operation is of "b = f(a,b)" kind

        public static BasicOp l(int code) {
            return CODE_LOOKUP.get(code);
        }

        BasicOp(String name, int code, int cycles, boolean modb) {
            this.name = name;
            this.code = code;
            this.cycles = cycles;
            this.modb = modb;
        }

        public static BasicOp byName(String name) {
            return NAME_LOOKUP.get(name);
        }
    }

    public enum SpecialOp {
        JSR("JSR", 0x01, 3, false),
        // HCF("HCF", 0x07, 9, false),
        INT("INT", 0x08, 4, false), IAG("IAG", 0x09, 1, true), IAS("IAS", 0x0a, 1, false), RFI("RFI", 0x0b, 3, false), IAQ("IAQ", 0x0c, 2, false),
        HWN("HWN", 0x10, 2, true), HWQ("HWQ", 0x11, 4, false), HWI("HWI", 0x12, 4, false);

        public final String name;
        public final int code;
        public final int cycles;
        public final boolean moda;

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

        SpecialOp(String name, int code, int cycles, boolean moda) {
            this.name = name;
            this.code = code;
            this.cycles = cycles;
            this.moda = moda;
        }
    }
    
    public enum OpType {
        BASIC, SPECIAL, INVALID;
        public static OpType getOpType(int cmd) {
            int o = (cmd & C_O_MASK);
            if (BasicOp.l(o) != null) return BASIC;

            int b = (cmd & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
            if (o == O_NBI && SpecialOp.l(b) != null) return SPECIAL;
            
            return INVALID;
        }
    }
    
    class Operation {
        int cmd;
        int opcode;
        int a;
        int b;
        OpType type;
        Operation(int cmd) {
            this.cmd = cmd;
            this.opcode = cmd & C_O_MASK;
            this.type = OpType.getOpType(cmd);
            switch (type) {
                case BASIC:
                    a = (cmd & C_A_MASK) >> C_A_SHIFT;
                    b = (cmd & C_B_MASK) >> C_B_SHIFT;                
                    break;
                case SPECIAL:
                    a = (cmd & C_NBI_A_MASK) >> C_NBI_A_SHIFT;
                    b = (cmd & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
                    break;
                default:
                    break;
            }
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
    public static final int O_MDI = BasicOp.MDI.code;
    public static final int O_AND = BasicOp.AND.code;
    public static final int O_BOR = BasicOp.BOR.code;
    public static final int O_XOR = BasicOp.XOR.code;
    public static final int O_SHR = BasicOp.SHR.code;
    public static final int O_ASR = BasicOp.ASR.code;
    public static final int O_SHL = BasicOp.SHL.code;
    public static final int O_IFB = BasicOp.IFB.code;
    public static final int O_IFC = BasicOp.IFC.code;
    public static final int O_IFE = BasicOp.IFE.code;
    public static final int O_IFN = BasicOp.IFN.code;
    public static final int O_IFG = BasicOp.IFG.code;
    public static final int O_IFA = BasicOp.IFA.code;
    public static final int O_IFL = BasicOp.IFL.code;
    public static final int O_IFU = BasicOp.IFU.code;
    // 0x1a-0x1f reserved
    public static final int O_ADX = BasicOp.ADX.code;
    public static final int O_SBX = BasicOp.SBX.code;
    public static final int O_STI = BasicOp.STI.code;
    public static final int O_STD = BasicOp.STD.code;
    // Special opcodes
    // public static final int O__HCF = SpecialOp.HCF.code;
    public static final int O__JSR = SpecialOp.JSR.code;
    public static final int O__INT = SpecialOp.INT.code;
    public static final int O__IAG = SpecialOp.IAG.code;
    public static final int O__IAS = SpecialOp.IAS.code;
    public static final int O__RFI = SpecialOp.RFI.code;
    public static final int O__IAQ = SpecialOp.IAQ.code;
    public static final int O__HWN = SpecialOp.HWN.code;
    public static final int O__HWQ = SpecialOp.HWQ.code;
    /*
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
    */
    // Register constants
    public static final int REGS_COUNT = Reg.values().length; ///< Number of registers

    // Command parts (opcode, A, B)
    public static final int C_O_BITLEN = 5;
    public static final int C_B_BITLEN = 5;
    public static final int C_A_BITLEN = 6;
    public static final int C_B_SHIFT = C_O_BITLEN;
    public static final int C_A_SHIFT = C_O_BITLEN + C_B_BITLEN;
    // to get a strip of N binary 1s, we do 1 << N and substract 1
    public static final int C_O_MASK = (1 << C_O_BITLEN) - 1;
    public static final int C_B_MASK = ((1 << C_B_BITLEN) - 1) << C_B_SHIFT;
    public static final int C_A_MASK = ((1 << C_A_BITLEN) - 1) << C_A_SHIFT;
    public static final int C_NBI_O_BITLEN = 5;
    public static final int C_NBI_A_BITLEN = 6;
    public static final int C_NBI_O_SHIFT = C_O_BITLEN;
    public static final int C_NBI_A_SHIFT = C_NBI_O_SHIFT + C_O_BITLEN;
    public static final int C_NBI_O_MASK = ((1 << C_NBI_O_BITLEN) - 1) << C_NBI_O_SHIFT;
    public static final int C_NBI_A_MASK = ((1 << C_NBI_A_BITLEN) - 1) << C_NBI_A_BITLEN;
    // Command value types (take one and shift with C_x_SHIFT)
    //   Plain register
    public static final int A_REG = 0;// | with REG_x
    public static final int A_A = 0;
    public static final int A_B = 1;
    public static final int A_C = 2;
    public static final int A_X = 3;
    public static final int A_Y = 4;
    public static final int A_Z = 5;
    public static final int A_I = 6;
    public static final int A_J = 7;
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
    public static final int A_M1 = A_CONST + 0;
    public static final int A_0 = A_CONST + 1;
    public static final int A_1 = A_CONST + 2;
    public static final int A_2 = A_CONST + 3;
    public static final int A_3 = A_CONST + 4;
    public static final int A_4 = A_CONST + 5;
    public static final int A_5 = A_CONST + 6;
    public static final int A_6 = A_CONST + 7;
    public static final int A_7 = A_CONST + 8;
    public static final int A_8 = A_CONST + 9;
    public static final int A_9 = A_CONST + 10;
    public static final int A_10 = A_CONST + 11;
    public static final int A_11 = A_CONST + 12;
    public static final int A_12 = A_CONST + 13;
    public static final int A_13 = A_CONST + 14;
    public static final int A_14 = A_CONST + 15;
    public static final int A_15 = A_CONST + 16;
    public static final int A_16 = A_CONST + 17;
    public static final int A_17 = A_CONST + 18;
    public static final int A_18 = A_CONST + 19;
    public static final int A_19 = A_CONST + 20;
    public static final int A_20 = A_CONST + 21;
    public static final int A_21 = A_CONST + 22;
    public static final int A_22 = A_CONST + 23;
    public static final int A_23 = A_CONST + 24;
    public static final int A_24 = A_CONST + 25;
    public static final int A_25 = A_CONST + 26;
    public static final int A_26 = A_CONST + 27;
    public static final int A_27 = A_CONST + 28;
    public static final int A_28 = A_CONST + 29;
    public static final int A_29 = A_CONST + 30;
    public static final int A_30 = A_CONST + 31;
    
    /*
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
    */

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
        if (!skip && stepListener != null) stepListener.preExecute(ppc);

        int cmd = mem[(mem[M_PC]++) & 0xffff] & 0xffff; // command value

        Operation op = new Operation(cmd);
        boolean postExecuteCalled = false;
        switch (op.type) {
            case BASIC:
                postExecuteCalled = handleBasicOp(op, skip);
                break;
            case SPECIAL:
                handleSpecialOp(op, skip);
                break;
            default:
                // invalid opcode
                reserved = true;
                halt = true;
                break;
        }
        
        for (Peripheral peripheral : peripherals) {
            peripheral.tick(cmd);
        }
        if (!postExecuteCalled && !skip && stepListener != null) stepListener.postExecute(ppc);
    }

    private boolean handleBasicOp(Operation op, boolean skip) {
        boolean postExecuteCalled = false;
        short ppc = mem[M_PC];
        short psp = mem[M_SP];

        BasicOp bop = BasicOp.l(op.opcode);
        
        int aa, ba, av, bv, asv, bsv;
        aa = getaddr(op.a, true) & 0x1ffff;
        ba = getaddr(op.b, false) & 0x1ffff;

        if (skip) {
            mem[M_SP] = psp;
            if (BasicOp.OPS_IF.contains(bop)) {
                // Chaining IF - skip one more instruction
                step(true);
            }
            return false;
        }

        asv = memget(aa);
        bsv = memget(ba);
        av = asv & 0xffff;
        bv = bsv & 0xffff;

        int rslt = mem[ba]; // new 'b' value
        int exreg = mem[M_EX]; // new 'EX' value
        boolean conditionalOpMiss = false;
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
            case MDI:
                // TODO: how is this different to MOD?
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
                if (av != bv) conditionalOpMiss = true;
                break;
            case IFN:
                if (av == bv) conditionalOpMiss = true;
                break;
            case IFG:
                if (!(bv > av)) conditionalOpMiss = true;
                break;
            case IFB:
                if ((av & bv) == 0) conditionalOpMiss = true;
                break;
            case IFC:
                if ((av & bv) != 0) conditionalOpMiss = true;
                break;
            case IFA:
                if (!(bsv > asv)) conditionalOpMiss = true;
                break;
            case IFL:
                if (!(bv < av)) conditionalOpMiss = true;
                break;
            case IFU:
                if (!(bsv < asv)) conditionalOpMiss = true;
                break;
            case ADX:
                rslt = bv + av + exreg;
                exreg = (rslt > 0xffff) ? 1 : 0;
                break;
            case SBX:
                rslt = bv - av + exreg;
                exreg = (rslt < 0) ? 0xffff : 0;
                break;
            case STI:
                rslt = av;
                mem[M_I]++;
                mem[M_J]++;
                break;
            case STD:
                rslt = av;
                mem[M_I]--;
                mem[M_J]--;
                break;
            default:
                throw new RuntimeException("DCPU Opcode not implemented: " + bop);
        }
        if (conditionalOpMiss) {
            postExecuteCalled = true;
            if (!skip && stepListener != null) stepListener.postExecute(ppc);
            step(true);
        }

        // overwrite 'b' unless it is constant
        if (ba < M_CV && bop.modb) memset(ba, (short) rslt);

        // only overwrite EX if it wasn't being changed itself with (e.g.) "SET EX, ..."
        if (ba != M_EX) mem[M_EX] = (short) exreg;
        
        return postExecuteCalled;
    }

    private void handleSpecialOp(Operation op, boolean skip) {
        // a,b: raw codes, addresses, values, signed values
        // in NBI: b stores NBO
        int aa, ba, av, bv, asv, bsv;
        short psp = mem[M_SP];

        aa = getaddr(op.a, true) & 0x1ffff;
        if (skip) {
            mem[M_SP] = psp;
            return;
        }
        ba = 0;
        asv = memget(aa);
        av = asv & 0xffff;
        bv = bsv = 0;

        int rslt = mem[aa]; // new 'a' value
        SpecialOp sop = SpecialOp.l(op.b);
        switch (sop) {
            case JSR:
                mem[(--mem[M_SP]) & 0xffff] = mem[M_PC];
                mem[M_PC] = (short) av;
                break;
            // case HCF:
            //    halt = true;
            //    break;
            case INT:
                // TODO INT
                break;
            case IAG:
                // TODO ING
                break;
            case IAS:
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
        // overwrite 'a' unless it is constant
        if (aa < M_CV && sop.moda) memset(aa, (short) rslt);
    
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
            mem[M_CV + i] = (short) ((i - 1) & 0xffff);
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
     * Generates command code for specified opcode, 'b', and 'a'.
     * <p/>
     * Example: gencmd(O_SET, A_PC, A_NW) for "set PC, next_word_of_ram"
     */
    public static short gencmd(int opcode, int b, int a) {
        if (opcode < 0 || opcode > 1 << C_O_BITLEN || b < 0 || b > 1 << C_B_BITLEN || a < 0 || a > 1 << C_A_BITLEN)
            throw new IllegalArgumentException("Bad arguments");
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
        if (addr < M_A) {
            return String.format("(%04x)", addr);
        } else if (addr < M_CV) {
            return Reg.l(addr - M_A).name;
        } else return String.valueOf(addr - M_CV - 1);//literal value
    }

    public String _dvalmem(int addr) {
        if (addr < M_A) {
            return String.format("(%04x) : %04x (%s)", addr, mem[addr], Character.toString((char) mem[addr]));
        } else if (addr < M_CV) {
            return Reg.l(addr - M_A).name;
        } else {
            return String.valueOf(addr - M_CV - 1);
        }
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
            // literal value.
            return M_CV + cmd - 0x20;
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
     */
    @Deprecated
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

    @Deprecated
    final Peripheral[] memlines = new Peripheral[16];
    @Deprecated
    final List<Peripheral> peripherals = new LinkedList<Peripheral>();

    @Deprecated
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

    @Deprecated
    public void detach(Peripheral peripheral) {
        if (peripheral.baseaddr != -1) {
            memlines[peripheral.baseaddr >> 12] = null;
        }
        peripherals.remove(peripheral);
        peripheral.detached();
    }
}
