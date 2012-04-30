package dcpu;

public class TestUtils {
    public static String charsToString(char[] data) {
        StringBuffer sb = new StringBuffer();
        for (char s : data) {
            sb.append(String.format("0x%04x ", (int) s));
        }
        return sb.toString();
    }

    public static void printChars(char[] data) {
        for (char s : data) {
            System.out.printf("0x%04x ", (int) s);
        }
        System.out.println();
    }

    public static String displayExpected(char[] expected, char[] actual) {
        return "expected " + TestUtils.charsToString(expected) + ", but got " + TestUtils.charsToString(actual);
    }

}
