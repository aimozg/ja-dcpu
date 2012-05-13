package dcpu.antlr.node;

import java.util.List;

import dcpu.Dcpu;
import dcpu.antlr.OpNode;

public class PushPopOpNode implements OpNode {

    @Override
    public int evaluate(List<Integer> nextWords) {
        return Dcpu.A_PUSHPOP;
    }
    
    @Override
    public String toString() {
        return "PUSH|POP";
    }
}
