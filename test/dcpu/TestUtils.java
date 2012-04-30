package dcpu;

public class TestUtils {
    public static String shortsToString(char[] data) {
        StringBuffer sb = new StringBuffer();
        for (char s : data) {
            sb.append(String.format("0x%04x ", (int) s));
        }
        return sb.toString();
    }

    public static void printShorts(short[] data) {
        for (short s : data) {
            System.out.printf("0x%04x ", s);
        }
        System.out.println();
    }

    public static String displayExpected(char[] expected, char[] actual) {
        return "expected " + TestUtils.shortsToString(expected) + ", but got " + TestUtils.shortsToString(actual);
    }

}
