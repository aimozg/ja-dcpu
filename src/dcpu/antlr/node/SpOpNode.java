package dcpu.antlr.node;

import java.util.List;

import dcpu.Dcpu;
import dcpu.antlr.OpNode;

public class SpOpNode implements OpNode {
    // Deals with all 3 cases of stack pointer access: SP, [SP], [SP + NW]
    
    private boolean hasNW;
    private boolean isMem;
    private int nextWord;

    public SpOpNode() {
        // SP
    }
    
    public SpOpNode(boolean isMem) {
        // isMem ? [SP] : SP
        this.isMem = isMem;
    }
    
    public SpOpNode(int nextWord, boolean isPositive) {
        // [SP +/- NW]
        if (!isPositive) {
            nextWord = 0x10000 - nextWord;
        }
        this.nextWord = nextWord;
        hasNW = true;
        isMem = true;
    }

    @Override
    public int evaluate(List<Integer> nextWords) {
        if (hasNW) {
            nextWords.add(0, nextWord);
            return Dcpu.A_PICK; 
        } else if (isMem) {
            return Dcpu.A_PEEK;
        } else {
            return Dcpu.A_SP;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("SP[hasNW: ")
          .append(hasNW)
          .append(", isMem: ")
          .append(isMem)
          .append(", nextWord: ")
          .append(nextWord)
          .append("]");
        return sb.toString();
    }

}
