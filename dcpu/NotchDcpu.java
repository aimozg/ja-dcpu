package dcpu;

import java.util.LinkedList;
import java.util.List;

/**
 * Notch's DCPU(tm)(c)(R)(ftw) specs v3 implementation.
 *
 * Created by IntelliJ IDEA.
 * Author: aimozg
 * Date: 28.03.12
 * Time: 19:15
 */
@SuppressWarnings({"UnusedDeclaration", "PointlessBitwiseExpression", "PointlessArithmeticExpression"})
public class NotchDcpu {

    ////////////////
    // NOTES
    ////////////////
    // * Registers are mapped to memory after addressable space for convenience (so operations take something from one
    //   mem cell and put something into another)
    // * Register O stores following values:
    //   * In ADD and SUB: 1 if were over/under-flow
    //   * In MUL : high word
    //   * In DIV : 1 if div by zero ('a' not modified)
    //   * In SHL : high word, leftmost 'b' bits of 'a'
    //   * In SHR : rightmost 'b' bits of 'a'
    // * NW is commonly used for 'Next Word in ram'
    // * Peripherals can be attached to monitor CPU ticks and writes to 4096-word memory lines (determined by highest nibble)
    // * Opcode 0 is commonly treated as "Halt"

    ////////////////
    /// CONSTANTS
    ////////////////

    //////
    // Opcode constants
    public static final int O_HLT = 0;
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
    public static final String[] OPCODE_NAMES = {
            "HLT","SET","ADD","SUB",
            "MUL","DIV","MOD","SHL",
            "SHR","AND","BOR","XOR",
            "IFE","IFN","IFG","IFB"
    };
    // operations that place their result into memory cell
    public static final boolean[] OPCODE_MODMEM = {
            false, true, true, true,
            true, true, true, true,
            true, true, true, true,
            false, false, false, false
    };
    // Register constants
    public static final int REG_A = 0;
    public static final int REG_B = 1;
    public static final int REG_C = 2;
    public static final int REG_X = 3;
    public static final int REG_Y = 4;
    public static final int REG_Z = 5;
    public static final int REG_I = 6;
    public static final int REG_J = 7;
    // Command parts (opcode, A, B)
    public static final int C_O_MASK = 0x000F;
    public static final int C_A_MASK = 0x03F0;
    public static final int C_B_MASK = 0xFC00;
    public static final int C_A_SHIFT = 4;
    public static final int C_B_SHIFT = 10;
    // Command address types (take one and shift with C_x_SHIFT)
    //   Plain register
    public static final int A_REG = 0;// | with REG_x
    public static final int A_A = A_REG | REG_A;
    public static final int A_B = A_REG | REG_B;
    public static final int A_C = A_REG | REG_C;
    public static final int A_X = A_REG | REG_X;
    public static final int A_Y = A_REG | REG_Y;
    public static final int A_Z = A_REG | REG_Z;
    public static final int A_I = A_REG | REG_I;
    public static final int A_J = A_REG | REG_J;
    //   (Register)
    public static final int A_M_REG = 8; // or with REG_x
    public static final int A_M_A = A_M_REG | REG_A;
    public static final int A_M_B = A_M_REG | REG_B;
    public static final int A_M_C = A_M_REG | REG_C;
    public static final int A_M_X = A_M_REG | REG_X;
    public static final int A_M_Y = A_M_REG | REG_Y;
    public static final int A_M_Z = A_M_REG | REG_Z;
    public static final int A_M_I = A_M_REG | REG_I;
    public static final int A_M_J = A_M_REG | REG_J;
    //  (Register+NW)
    public static final int A_M_NW_REG = 16; // or with REG_x
    public static final int A_M_NW_A = A_M_NW_REG | REG_A;
    public static final int A_M_NW_B = A_M_NW_REG | REG_B;
    public static final int A_M_NW_C = A_M_NW_REG | REG_C;
    public static final int A_M_NW_X = A_M_NW_REG | REG_X;
    public static final int A_M_NW_Y = A_M_NW_REG | REG_Y;
    public static final int A_M_NW_Z = A_M_NW_REG | REG_Z;
    public static final int A_M_NW_I = A_M_NW_REG | REG_I;
    public static final int A_M_NW_J = A_M_NW_REG | REG_J;
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
    public static final int A_0 = A_CONST+0;
    public static final int A_1 = A_CONST+1;
    public static final int A_2 = A_CONST+2;
    public static final int A_3 = A_CONST+3;
    public static final int A_4 = A_CONST+4;
    public static final int A_5 = A_CONST+5;
    public static final int A_6 = A_CONST+6;
    public static final int A_7 = A_CONST+7;
    public static final int A_8 = A_CONST+8;
    public static final int A_9 = A_CONST+9;
    public static final int A_10 = A_CONST+10;
    public static final int A_11 = A_CONST+11;
    public static final int A_12 = A_CONST+12;
    public static final int A_13 = A_CONST+13;
    public static final int A_14 = A_CONST+14;
    public static final int A_15 = A_CONST+15;
    public static final int A_16 = A_CONST+16;
    public static final int A_17 = A_CONST+17;
    public static final int A_18 = A_CONST+18;
    public static final int A_19 = A_CONST+19;
    public static final int A_20 = A_CONST+20;
    public static final int A_21 = A_CONST+21;
    public static final int A_22 = A_CONST+22;
    public static final int A_23 = A_CONST+23;
    public static final int A_24 = A_CONST+24;
    public static final int A_25 = A_CONST+25;
    public static final int A_26 = A_CONST+26;
    public static final int A_27 = A_CONST+27;
    public static final int A_28 = A_CONST+28;
    public static final int A_29 = A_CONST+29;
    public static final int A_30 = A_CONST+30;
    public static final int A_31 = A_CONST+31;

    //////
    // Register addresses
    public static final int M_A = 0x10000;
    public static final int M_B = 0x10001;
    public static final int M_C = 0x10002;
    public static final int M_X = 0x10003;
    public static final int M_Y = 0x10004;
    public static final int M_Z = 0x10005;
    public static final int M_I = 0x10006;
    public static final int M_J = 0x10007;
    public static final int M_PC= 0x10008;
    public static final int M_SP= 0x10009;
    public static final int M_O = 0x1000a;
    public static final int M_CV= 0x1000b; // constant value
    // Memory cell names
    public static final String[] MEM_NAMES = {
            "A","B","C","X","Y","Z","I","J",
            "PC","SP","O",
            "0","1","2","3","4","5","6","7",
            "8","9","10","11","12","13","14","15",
            "16","17","18","19","20","21","22","23",
            "24","25","26","27","28","29","30","31"
    };

    ///////////////////////////////////////////////////////////////
    // CORE CPU FUNCTIONS
    ///////////////////////////////////////////////////////////////

    // Memory cells: 64k RAM + 8 general-purpose regs + SP + PC + O + 32 constants
    public final short[] mem = new short[0x10000+8+3+32];
    public boolean reserved = false; // true if operation 0 executed

    /**
     * Runs until hitting Opcode 0
     */
    public void run(){
        while (!reserved){
            step(false);
        }
    }

    /**
     * Execute one operation (skip = false) or skip one operation.
     */
    public void step(boolean skip){
        short sp0 = mem[M_SP]; // save SP
        int cmd = mem[(mem[M_PC]++)&0xffff]; // command value
        int opcode = cmd& C_O_MASK;
        // a,b: raw codes, addresses, values
        int a = (cmd& C_A_MASK)>> C_A_SHIFT;
        int b = (cmd& C_B_MASK)>> C_B_SHIFT;
        int aa = getaddr(a)&0x1ffff;
        int ba = getaddr(b)&0x1ffff;
        int av = mem[aa]&0xffff;
        int bv = mem[ba]&0xffff;

        // debug
        //_dstep(skip, opcode, aa, ba, av, bv);

        if (skip){
            mem[M_SP] = sp0; // restore SP that could be modified in getaddr()
            return;
        }
        int rslt = mem[aa]; // new 'a' value
        int oreg = mem[M_O]; // new 'O' value
        switch(opcode){
            case O_HLT:
                reserved = true;
                break;
            case O_SET:
                rslt = mem[ba];
                break;
            case O_ADD:
                rslt =  av+bv;
                oreg = (rslt>0xffff)?1:0;
                break;
            case O_SUB:
                rslt = av-bv;
                oreg = (rslt<0)?1:0;
                break;
            case O_MUL:
                rslt = av*bv;
                oreg = rslt>>16;
                break;
            case O_DIV:
                if (bv == 0){
                    oreg = 1;
                } else {
                    rslt = (short) (av/bv);
                }
                break;
            case O_MOD:
                rslt = (short) (av%bv);
                break;
            case O_SHL:
                rslt = av<<bv;
                oreg = rslt>>16;
                break;
            case O_SHR:
                rslt = av>>bv;
                oreg = av-(rslt<<bv);
                break;
            case O_AND:
                rslt = av&bv;
                break;
            case O_BOR:
                rslt = av|bv;
                break;
            case O_XOR:
                rslt = av^bv;
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
                if ((av & bv)==0) step(true);
                break;
        }
        // overwrite 'a' unless it is constant
        if (aa<M_CV && OPCODE_MODMEM[opcode]) memset(aa, (short) rslt);
        mem[M_O] = (short) oreg;
        for (Peripheral peripheral : peripherals) {
            peripheral.tick(cmd);
        }
    }

    /**
     * Sets memory[addr] to value, calling peripheral hook, if installed
     */
    public void memset(int addr, short value) {
        short oldval = mem[addr];
        int line = addr>>12;
        mem[addr] = value;
        if (line>=0 && line<memlines.length && memlines[line] != null){
            memlines[line].onMemset(addr,value,oldval);
        }
    }

    /**
     * Sets memory[addr] to value, *without* calling peripheral hook
     */
    public void memset_raw(int addr, short value){
        mem[addr] = value;
    }


    public void reset(){
        reserved = false;
        for (int i = 0; i<8+3; i++) mem[M_A+i] = 0;
        for (int i = 0; i<32; i++){
            mem[M_CV+i]=(short)i;
        }
    }

    public NotchDcpu() {
        reset();
    }

    ////////////////////////////////////////////
    // UTILITY, DEBUG, AND INTERNAL FUNCTIONS

    /**
     * Generates command code for specified opcode, 'a', and 'b'.
     *
     * Example: gencmd(O_SET, A_PC, A_NW) for "set PC, next_word_of_ram"
     */
    public static short gencmd(int opcode,int a,int b){
        return (short) (opcode | a<<C_A_SHIFT | b<<C_B_SHIFT);
    }

    // debug
    private void _d(String ln,Object... args){
        System.out.printf(ln, args);
    }

    /**
     * List of all registers with their hex values
     */
    public String _dregs(){
        return String.format("R A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x  PC=%04x SP=%04x O=%04x",
                mem[M_A], mem[M_B], mem[M_C], mem[M_X], mem[M_Y], mem[M_Z], mem[M_I], mem[M_J],
                mem[M_PC], mem[M_SP], mem[M_O]);
    }
    private String _dmem(int addr){
        return (addr<M_A)?String.format("(%04x)",addr):MEM_NAMES[addr-M_A];
    }
    private void _dstep(boolean skip, int opcode, int aa, int ba, int av, int bv) {
        _d("%s%s %s=%04x %s=%04x\n",skip?"; ":"> ",OPCODE_NAMES[opcode],_dmem(aa),av,_dmem(ba),bv);
    }

    /**
     * Returns memory address for operand code. 0 returns address of register A and so on.
     * May modify values of PC (in case of "next word of ram") and SP (when PUSH, POP)
     */
    private int getaddr(int cmd){
        if (cmd<=7){
            return M_A+cmd;
        } else if (cmd<=15){
            return mem[M_A+cmd-8]&0xffff;
        } else if (cmd<=23){
            return (mem[M_A+cmd-16]+mem[mem[M_PC]++])&0xffff;
        } else if (cmd>=32){
            return M_CV+cmd-32;
        } else switch (cmd){
            case 24:
                return (mem[M_SP]++)&0xffff;
            case 25:
                return mem[M_SP]&0xffff;
            case 26:
                return (--mem[M_SP])&0xffff;
            case 27:
                return M_SP;
            case 28:
                return M_PC;
            case 29:
                return M_O;
            case 30:
                return mem[mem[M_PC]++]&0xffff;
            case 31:
                return mem[M_PC]++&0xffff;
            default:
                throw new RuntimeException("THIS SHOULD NEVER HAPPEN");
        }
    }

    public void upload(short[] buffer, int srcoff, int len, int dstoff) {
        if (srcoff>=0x10000 || srcoff<0 || len<0 || srcoff+len>=0x10000) throw new IllegalArgumentException("Bad offset/length");
        System.arraycopy(buffer,srcoff,mem,dstoff,len);
    }

    public void upload(short[] buffer) {
        upload(buffer,0,buffer.length,0);
    }


    ///////////////////////////////////////////////////////////////////////////
    //// PERIPHERAL

    /**
     * Peripheral to DCPU.
     *
     * Communication
     */
    public abstract static class Peripheral {

        public NotchDcpu cpu;
        public int line;

        /**
         * This method is called every CPU cycle. cmd is last command code
         */
        public void tick(int cmd){

        }

        /**
         * This method is called when program or other peripheral writes "newval" to
         * memory address "addr". Method is called only for this
         */
        public void onMemset(int addr,short newval,short oldval){

        }

        /**
         * Called when attached to cpu
         */
        public void attachedTo(NotchDcpu cpu, int line) {
            this.cpu = cpu;
            this.line = line;
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

    public void attach(Peripheral peripheral,int line){
        if (line != -1){
            if (memlines[line] != null){
                throw new IllegalStateException("Peripheral already attached to line");
            }
            memlines[line] = peripheral;
        }
        peripherals.add(peripheral);
        peripheral.attachedTo(this,line);
    }

    public void detach(Peripheral peripheral){
        if (peripheral.line != -1){
            memlines[peripheral.line] = null;
        }
        peripherals.remove(peripheral);
        peripheral.detached();
    }
}
