package dcpu.antlr.node;

import java.util.List;

import dcpu.Dcpu;
import dcpu.antlr.OpNode;

public class RegOpNode implements OpNode {
    // Deals with all following cases of register access: Register, [Register], [Register + NW], but NOT [Register + label]
    
    private String register;
    private boolean hasNW;
    private boolean isMem;
    private int nextWord;

    public RegOpNode(String register) {
        // Register
        this.register = register;
    }
    
    public RegOpNode(String register, boolean isMem) {
        // isMem ? [Register] : Register
        this.register = register;
        this.isMem = isMem;
    }
    
    public RegOpNode(String register, int nextWord, boolean isPositive) {
        // [Register +/- NW]
        if (!isPositive) {
            nextWord = 0x10000 - nextWord;
        }
        this.register = register;
        this.nextWord = nextWord;
        hasNW = true;
    }
    
    @Override
    public int evaluate(List<Integer> nextWords) {
        if (hasNW) {
            nextWords.add(0, nextWord);
            return (Dcpu.Reg.byName(register).offset | Dcpu.A_M_NW_REG); 
        } else if (isMem) {
            return (Dcpu.Reg.byName(register).offset | Dcpu.A_M_REG);
        } else {
            return Dcpu.Reg.byName(register).offset;
        }
    }
    
    public void setNextWord(int nextWord) {
        this.nextWord = nextWord;
        hasNW = true;
    }

    @Override
    public int getLength() {
        return hasNW ? 1 : 0;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Register[name: ")
          .append(register)
          .append(", hasNW: ")
          .append(hasNW)
          .append(", isMem: ")
          .append(isMem)
          .append(", nextWord: ")
          .append(nextWord)
          .append("]");
        return sb.toString();
    }

}
