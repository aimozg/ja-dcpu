package dcpu.antlr;

import java.util.List;

public interface OpNode {
    int evaluate(List<Integer> nextWords);
    int getLength(); // length of *extra* words required for this op
}