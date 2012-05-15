package dcpu.antlr;

public class Label {
    public String name;
    public int instructionIndex;
    
    public Label(String name) {
        this.name = name;
        this.instructionIndex = -1;
    }
    
    @Override
    public String toString() {
        return "Label[name: " + name + ", index: " + instructionIndex + "]";
    }
}