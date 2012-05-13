package dcpu.antlr.node;

import java.util.List;

public class ReferenceOpNode extends ValueOpNode {
    // This is similar to a ValueOpNode, just we don't yet know what its value is
    // SET A, label1
    // SET A, [0x8000 + label2]
    // DAT label3
    
    private String name;
    private boolean isResolved;

    public ReferenceOpNode(String name, boolean isMem) {
        // isMem ? [label] : label
        super(-1, isMem);
        this.name = name;
        isResolved = false;
    }
    
    @Override
    public int evaluate(List<Integer> nextWords) {
        if (!isResolved) throw new UnsupportedOperationException(String.format("Cannot evaluate yet, reference %s is unresolved", name));
        return super.evaluate(nextWords);
    }
    
    public void setValue(int value) {
        this.value = value; 
        isResolved = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Reference[name: ")
          .append(name)
          .append(", isResolved: ")
          .append(isResolved)
          .append(", valueOp: ")
          .append(super.toString())
          .append("]");
        return sb.toString();
    }

}
