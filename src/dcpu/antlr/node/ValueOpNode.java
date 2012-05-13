package dcpu.antlr.node;

import java.util.List;

import dcpu.Dcpu;
import dcpu.antlr.OpNode;

public class ValueOpNode implements OpNode {
    // deals with value and [value], also allows for short literals
    protected int value;
    protected boolean isMem;
    protected boolean canBeShort;

    public ValueOpNode(int value) {
        this.value = value;
        isMem = false;
        canBeShort = false;
    }
    
    public ValueOpNode(int value, boolean isMem) {
        // isMem ? [value] : value.
        this.value = value;
        this.isMem = isMem;
        canBeShort = false; // canBeShort is decided later before evaluation as an optimization
    }

    @Override
    public int evaluate(List<Integer> nextWords) {
        if (isMem) {
            nextWords.add(0, value);
            return Dcpu.A_M_NW;
        }
        if (canBeShort && (value >= -1 && value <= 30)) {
            return Dcpu.A_0 + value;
        } else {
            nextWords.add(0, value);
            return Dcpu.A_NW;
        }
    }
    
    public void setCanBeShort(boolean canBeShort) {
        this.canBeShort = canBeShort;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Value[value: ")
          .append(value)
          .append(", isMem: ")
          .append(isMem)
          .append(", canBeShort: ")
          .append(canBeShort)
          .append("]");
        return sb.toString();
    }

}
