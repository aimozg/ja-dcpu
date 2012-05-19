package dcpu.antlr.node;

import java.util.List;

import dcpu.Dcpu;
import dcpu.antlr.OpNode;

public class ValueOpNode implements OpNode {
    // deals with value and [value], also allows for short literals
    public int value;
    public boolean isMem;
    public boolean canBeShort; // only source op values can be short, this is set when instruction is created
    public boolean isShortDecided; // do we know yet if this can be short or not? B will always be true (can't be short), A we have to work out
    public boolean needsValue; // for cases where we know it can't be short, but don't yet know what its value is, e.g. long references

    public ValueOpNode(int value) {
        this.value = value;
        isMem = false;
        checkShort();
    }

    public ValueOpNode(boolean isMem) {
        this.isMem = isMem;
        if (isMem) {
            canBeShort = false;
            isShortDecided = true;
        }
        needsValue = true;
    }
    
    public ValueOpNode(int value, boolean isMem) {
        // isMem ? [value] : value.
        this.value = value;
        this.isMem = isMem;
        checkShort();
    }

    protected void checkShort() {
        // this isn't called if we don't know the value
        if (isMem || !(value >= -1 && value <= 30)) {
            canBeShort = false;
            isShortDecided = true;
        } else {
            canBeShort = true;
            isShortDecided = true;
        }
    }
    
    @Override
    public int evaluate(List<Integer> nextWords) {
        if (needsValue) {
            throw new UnknownValueException("Cannot evaluate value yet, no value has been set");
        }
        if (isMem) {
            nextWords.add(0, value);
            return Dcpu.A_M_NW;
        }
        if (isShortDecided && canBeShort) {
            return Dcpu.A_0 + value;
        } else if (isShortDecided && !canBeShort) {
            nextWords.add(0, value);
            return Dcpu.A_NW;
        } else {
            throw new UnknownValueException("Cannot evaluate value until decided if this is a short yet");
        }
    }
    
    @Override
    public int getLength() {
        if (isMem) {
            return 1;
        }
        if (isShortDecided) {
            return canBeShort ? 0 : 1;
        } else {
            throw new UnknownValueException("Unknown length. The value hasn't been decided yet."); 
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Value[value: ");
        
        if (needsValue) {
            sb.append("<unknown>");
        } else {
            sb.append(value);
        }
        sb.append(", isMem: ")
          .append(isMem)
          .append(", canBeShort: ")
          .append(canBeShort)
          .append(", isShortDecided: ")
          .append(isShortDecided)
          .append(", length: ");
        if (isShortDecided) {
            sb.append(getLength());
        } else {
            sb.append("<unknown>");
        }
        sb.append("]");
        return sb.toString();
    }

}
