package dcpu;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Assembler map, which contains label-address and line-address maps, and also code/data bitmap
 */
public class AsmMap {
    public final Map<String, Short> symbolMap = new HashMap<String, Short>();
    public final Map<Integer, Short> srcMap = new HashMap<Integer, Short>();
    public final Map<Short, Integer> binMap = new HashMap<Short, Integer>();
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
    public Short symbol(String name) {
        return symbolMap.get(name);
    }

    /**
     * Returns binary address, corresponding to source line
     */
    public Short src2bin(int srcline) {
        return srcMap.get(srcline);
    }

    /**
     * Returns source line, corresponding to binary address
     */
    public Integer bin2src(short addr) {
        return binMap.get(addr);
    }
}
