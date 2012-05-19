package dcpu.antlr;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dcpu.antlr.node.ReferenceOpNode;

public class LabelTable {
    public Map<String, Label> labels = new HashMap<String, Label>();
    public List<ReferenceOpNode> references = new LinkedList<ReferenceOpNode>();
    
    public void define(String labelName) {
        Label label = new Label(labelName);
        labels.put(label.name.toUpperCase(), label);
    }
    
    public Label resolve(String name) {
        return labels.get(name.toUpperCase());
    }

    public void setLabelIndex(String labelName, int index) {
        Label l = resolve(labelName);
        if (l == null) {
            System.err.printf("ERROR: trying to set index for label %s that is undefined\n", labelName);
        } else {
            l.instructionIndex = index;
        }
    }

    public ReferenceOpNode createReference(String labelName, boolean isMem, String register) {
        if (resolve(labelName) == null) {
            System.err.printf("ERROR: Trying to make reference to unknown label: %s\n", labelName);
            return null;
        }
        ReferenceOpNode newRef = ReferenceOpNode.createReference(labelName, isMem, register);
        references.add(newRef);
        return newRef;
    }

    public void fillReferenceIndexes() {
        for (ReferenceOpNode reference : references) {
            Label label = resolve(reference.labelName);
            if (label == null) {
                System.err.printf("ERROR: Did not find a label for reference %s\n", reference.labelName);
            } else {
                reference.resolve(label.instructionIndex); 
            }
        }
    }
}
