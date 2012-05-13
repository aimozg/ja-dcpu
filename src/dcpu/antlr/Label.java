package dcpu.antlr;

public class Label {
    public String name;
    public int instructionIndex;
    
    public Label(String name, int index) {
        this.name = name;
        this.instructionIndex = index;
    }
    
    @Override
    public String toString() {
        return "Label[name: " + name + ", index: " + instructionIndex + "]";
    }
}