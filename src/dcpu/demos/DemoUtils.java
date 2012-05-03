package dcpu.demos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

public class DemoUtils {
    public static Reader getDemoAsmReader(Class<?> c, String postFix) {
        String asmSrcPath = c.getSimpleName() + postFix + ".asm";
        return new BufferedReader(new InputStreamReader(c.getResourceAsStream(asmSrcPath)));
    }
    public static Reader getDemoAsmReader(Class<?> c) {
        return getDemoAsmReader(c, "");
    }
}
