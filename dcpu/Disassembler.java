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

    public String next() {
        int instr = mem[address++] & 0xffff;
        int opcode = instr & C_O_MASK;
        if (opcode != O_NBI) {
            int a = (instr & C_A_MASK) >> C_A_SHIFT;
            int b = (instr & C_B_MASK) >> C_B_SHIFT;
            return OPCODE_NAMES[opcode] + " " + operand(a) + ", " + operand(b);
        } else {
            int a = (instr & C_NBI_A_MASK) >> C_NBI_A_SHIFT;
            opcode = (instr & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
            if (OPCODE0_RESERVED[opcode]) {
                return String.format("DAT %04x", instr);
            } else {
                return OPCODE0_NAMES[opcode] + " " + operand(a);
            }
        }
    }

    String operand(int code) {
        if (code <= 7) {
            return MEM_NAMES[code];
        } else if (code <= 15) {
            return "[" + MEM_NAMES[code - 8] + "]";
        } else if (code <= 23) {
            return String.format("[%s+0x%04x]", MEM_NAMES[code - 16], mem[address++]);
        } else if (code >= 32) {
            return String.valueOf(code - 32);
        } else switch (code) {
            case 24:
                return "POP";
            case 25:
                return "SP";
            case 26:
                return "PUSH";
            case 27:
                return "SP";
            case 28:
                return "PC";
            case 29:
                return "O";
            case 30:
                return String.format("[%04x]", mem[address++]);
            case 31:
                return String.format("%04x", mem[address++]);
            default:
                throw new RuntimeException("THIS SHOULD NEVER HAPPEN");
        }
    }

}
