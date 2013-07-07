package dcpu.demos;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class DemoUtils {
    public static Reader getDemoAsmReader(Class<?> c, String postFix) {
        String asmSrcPath = c.getSimpleName() + postFix + ".asm";
        InputStream stream = c.getResourceAsStream(asmSrcPath);
        return new BufferedReader(new InputStreamReader(stream));
    }

    public static Reader getDemoAsmReader(Class<?> c) {
        return getDemoAsmReader(c, "");
    }
}
