package dcpu;

import java.util.ArrayList;
import java.util.List;

import static dcpu.Dcpu.*;

/**
 * Converts opcodes to strings
 */
public class Disassembler {

    int address;
    private char[] mem;

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public void init(char[] mem) {
        this.mem = mem;
    }

    public String next(boolean incrementMemory) {
        List<String> nextWords = new ArrayList<String>();
        int instr = (incrementMemory ? mem[address++] : mem[address]);
        int opcode = instr & C_O_MASK;
        if (opcode != O_NBI) {
            int a = (instr & C_A_MASK) >> C_A_SHIFT;
            int b = (instr & C_B_MASK) >> C_B_SHIFT;
            BasicOp bop = BasicOp.l(opcode);

            // Words come in order : OPERATION_WORD NW_A NW_B
            // but we print B first, so need to store up the NW_ values before printing them in case there's multiple
            StringBuilder sb = new StringBuilder();
            sb.append((bop == null) ? "???" : bop.name).append(' ');
            operand(b, sb, nextWords, false);
            sb.append(", ");
            operand(a, sb, nextWords, true);
            String line = sb.toString();
            int nextWordIndex = 0;
            for (String nextWord : nextWords) {
                String replaceString = String.format("__NEXT_WORD_%d__", ++nextWordIndex);
                line = line.replace(replaceString, nextWord);
            }
            return line;
        } else {
            int a = (instr & C_NBI_A_MASK) >> C_NBI_A_SHIFT;
            opcode = (instr & C_NBI_O_MASK) >> C_NBI_O_SHIFT;
            SpecialOp sop = SpecialOp.l(opcode);
            if (sop == null) return String.format("DAT 0x%04x", instr);

            StringBuilder sb = new StringBuilder();
            sb.append(sop.name).append(' ');
            operand(a, sb, nextWords, true);
            String line = sb.toString();
            int nextWordIndex = 0;
            for (String nextWord : nextWords) {
                String replaceString = String.format("__NEXT_WORD_%d__", ++nextWordIndex);
                line = line.replace(replaceString, nextWord);
            }
            return line;
        }
    }

    void operand(int code, StringBuilder sb, List<String> nextWords, boolean isA) {
        if (code <= 0x07) {
            sb.append(Reg.l(code).name);
        } else if (code <= 0x0F) {
            sb.append("[" + Reg.l(code - 8).name + "]");
        } else if (code <= 0x17) {
            nextWords.add(0, String.format("0x%04x", (int) mem[address++]));
            sb.append(String.format("[%s + __NEXT_WORD_%d__]", Reg.l(code - 16).name, nextWords.size()));
        } else if (code >= 0x20 && code <= 0x3F) {
            sb.append(String.valueOf(code - 0x20 - 1));
        } else switch (code) {
            case A_PUSHPOP:
                sb.append(isA ? "POP" : "PUSH");
                break;
            case A_PEEK:
                sb.append("PEEK");
                break;
            case A_PICK:
                nextWords.add(0, String.format("%d", (int) mem[address++]));
                sb.append(String.format("PICK __NEXT_WORD_%d__", nextWords.size()));
                break;
            case A_SP:
                sb.append("SP");
                break;
            case A_PC:
                sb.append("PC");
                break;
            case A_EX:
                sb.append("EX");
                break;
            case A_M_NW:
                nextWords.add(0, String.format("0x%04x", (int) mem[address++]));
                sb.append(String.format("[__NEXT_WORD_%d__]", nextWords.size()));
                break;
            case A_NW:
                nextWords.add(0, String.format("0x%04x", (int) mem[address++]));
                sb.append(String.format("__NEXT_WORD_%d__", nextWords.size()));
                break;
            default:
                throw new RuntimeException("Unknown code value: " + code);
        }
    }

}
