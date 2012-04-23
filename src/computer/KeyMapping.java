package computer;

import java.util.HashMap;
import java.util.Map;

/*
 * Taken from Notch's disassembled application
 */
public class KeyMapping {

    public KeyMapping() {
        keyMap = new HashMap<Integer, Integer>();
    }

    public int getKey(int key) {
        if (keyMap.containsKey(Integer.valueOf(key)))
            return ((Integer) keyMap.get(Integer.valueOf(key))).intValue();
        else
            return -1;
    }

    protected void map(int key, int c) {
        keyMap.put(Integer.valueOf(key), Integer.valueOf(c));
    }

    public Map<Integer, Integer> keyMap;
}