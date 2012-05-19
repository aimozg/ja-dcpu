package dcpu.antlr.node;

import java.util.List;

import dcpu.Dcpu;
import dcpu.antlr.OpNode;

public class PcOpNode implements OpNode {

    @Override
    public int evaluate(List<Integer> nextWords) {
        return Dcpu.A_PC;
    }

    @Override
    public String toString() {
        return "PC";
    }

    @Override
    public int getLength() {
        return 0;
    }
    
}
