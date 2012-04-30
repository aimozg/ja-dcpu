package dcpu;

import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Assembler map, which contains label-address and line-address maps, and also code/data bitmap
 */
public class AsmMap {
    public final Map<String, Character> symbolMap = new TreeMap<String, Character>();
    public final Map<Integer, Character> srcMap = new TreeMap<Integer, Character>();
    public final Map<Character, Integer> binMap = new TreeMap<Character, Integer>();
    public final BitSet code = new BitSet();

    /**
     * true if addr contains code (not necessary instruction start)
     */
    public boolean code(int addr) {
        return code.get(addr & 0xffff);
    }

    /**
     * true if addr contains data
     */
    public boolean data(int addr) {
        return !code.get(addr & 0xffff);
    }

    /**
     * Returns symbol value by name
     */
    public Character symbol(String name) {
        return symbolMap.get(name);
    }

    /**
     * Returns binary address, corresponding to source line
     */
    public Character src2bin(int srcline) {
        return srcMap.get(srcline);
    }

    /**
     * Returns source line, corresponding to binary address
     */
    public Integer bin2src(char addr) {
        return binMap.get(addr);
    }
}
