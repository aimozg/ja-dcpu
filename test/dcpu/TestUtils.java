
package dcpu;

public class TestUtils {
    public static String shortsToString(short[] data) {
        StringBuffer sb = new StringBuffer();
        for (short s : data) {
            sb.append(String.format("0x%04x ", s));
        }
        return sb.toString();
    }

    public static void printShorts(short[] data) {
        for (short s : data) {
            System.out.printf("0x%04x ", s);
        }
        System.out.println();
    }
    
    public static String displayExpected(short[] expected, short[] actual) {
        return "expected " + TestUtils.shortsToString(expected) + ", but got " + TestUtils.shortsToString(actual);
    }
    
}
