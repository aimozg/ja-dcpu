package dcpu.antlr.node;

import java.util.List;

public class ReferenceOpNode extends ValueOpNode {
    // This is similar to a ValueOpNode, just we don't yet know what its value is.
    // SET A, label1
    // SET A, [0x8000 + label2]
    // DAT label3
	// SET A, [label4]
    // SET A, [X + label5]
    
    public String labelName;
    public String register;
    public boolean isResolved;

    public ReferenceOpNode(String labelName, boolean isMem) {
        // isMem ? [label] : label
        super(-1, isMem);
        this.labelName = labelName;
    }
    
    public ReferenceOpNode(String labelName, String register) {
        // [X + label]
        super(-1, true);
        this.labelName = labelName;
        this.register = register;
    }
    
    @Override
    public int evaluate(List<Integer> nextWords) {
        if (!isResolved) throw new UnsupportedOperationException(String.format("Cannot evaluate yet, reference %s is unresolved", labelName));
    	return super.evaluate(nextWords);
    }
    
    public void resolve(int value) {
        this.value = value;
        isResolved = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Reference[label: ")
          .append(labelName)
          .append(", isResolved: ")
          .append(isResolved)
          .append(", register: ")
          .append(register == null ? "<none>" : register)
          .append(", valueOp: ")
          .append(super.toString())
          .append("]");
        return sb.toString();
    }

    public static ReferenceOpNode createReference(String labelName, boolean isMem, String register) {
        ReferenceOpNode newRef;
        if (register == null) {
            newRef = new ReferenceOpNode(labelName, isMem);
        } else {
            newRef = new ReferenceOpNode(labelName, register);
        }
        return newRef;
    }

}
