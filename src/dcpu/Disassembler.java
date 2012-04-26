package dcpu;

import static dcpu.Dcpu.*;

/**
 * Converts opcodes to strings
 */
public class Disassembler {

    int address;
    private short[] mem;

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public void init(short[] mem) {
        this.mem = mem;
    }

    public String next(boolean incrementMemory) {
        int instr = (incrementMemory ? mem[address++] : mem[address]) & 0xffff;
        int opcode = instr & C_O_MASK;
        if (opcode != O_NBI) {
            int a = (instr & C_A_MASK) >> C_A_SHIFT;
            int b = (instr & C_B_MASK) >> C_B_SHIFT;
            BasicOp bop = BasicOp.l(opcode);
            return ((bop == null) ? "???" : bop.name) + " " + operand(b) + ", " + operand(a);
        } else {
            int a = (instr & C_NBI_A_MASK) >> C_NBI_A_SHIFT;
            opcode = (instr & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
            SpecialOp sop = SpecialOp.l(opcode);
            if (sop == null) return String.format("DAT 0x%04x", instr);
            else return sop.name + " " + operand(a);
        }
    }

    String operand(int code) {
        if (code <= 0x07) {
            return Reg.l(code).name;
        } else if (code <= 0x0F) {
            return "[" + Reg.l(code - 8).name + "]";
        } else if (code <= 0x17) {
            return String.format("[%s+0x%04x]", Reg.l(code - 16).name, mem[address++]);
        } else if (code >= 0x20 && code <= 0x3F) {
            return String.valueOf(code - 0x20 - 1);
        } else switch (code) {
            case A_PUSHPOP://TODO
                return "PUSHPOP";
            case A_PEEK:
                return "PEEK";
            case A_PICK:
                return "PICK";
            case A_SP:
                return "SP";
            case A_PC:
                return "PC";
            case A_EX:
                return "EX";
            case A_M_NW:
                return String.format("[0x%04x]", mem[address++]);
            case A_NW:
                return String.format("0x%04x", mem[address++]);
            default:
                throw new RuntimeException("Unknown code value: " + code);
        }
    }

}
