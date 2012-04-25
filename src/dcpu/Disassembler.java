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
            return OPCODE_NAMES[opcode] + " " + operand(a) + ", " + operand(b);
        } else {
            int a = (instr & C_NBI_A_MASK) >> C_NBI_A_SHIFT;
            opcode = (instr & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
            if (OPCODE0_RESERVED[opcode]) {
                return String.format("DAT 0x%04x", instr);
            } else {
                return OPCODE0_NAMES[opcode] + " " + operand(a);
            }
        }
    }

    String operand(int code) {
        if (code <= 0x07) {
            return MEM_NAMES[code];
        } else if (code <= 0x0F) {
            return "[" + MEM_NAMES[code - 8] + "]";
        } else if (code <= 0x17) {
            return String.format("[%s+0x%04x]", MEM_NAMES[code - 16], mem[address++]);
        } else if (code >= 0x20 && code <= 0x3F) {
            return String.valueOf(code - 0x20);
        } else switch (code) {
            case 0x18:
                return "POP";
            case 0x19:
                return "PEEK";
            case 0x1A:
                return "PUSH";
            case 0x1B:
                return "SP";
            case 0x1C:
                return "PC";
            case 0x1D:
                return "EX";
            case 0x1E:
                return String.format("[0x%04x]", mem[address++]);
            case 0x1F:
                return String.format("0x%04x", mem[address++]);
            default:
                throw new RuntimeException("Unknown code value: " + code);
        }
    }

}
