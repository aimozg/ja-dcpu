package dcpu.demos;

import dcpu.DcpuCompiler;
import dcpu.NotchDcpu;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 03.04.12
 * Time: 0:59
 */
public class CompilerDemo {
    public static void main(String[] args) {
        DcpuCompiler compiler = new DcpuCompiler();
        short[] bytecode = compiler.compile(";1 Assembler test for DCPU\n" +
                ";2 by Markus Persson\n" +
                "\n" +
                "             set a, 0xbeef                        ;4 Assign 0xbeef to register a\n" +
                "             set [0x1000], a                      ;5 Assign memory at 0x1000 to value of register a\n" +
                "             ifn a, [0x1000]                      ;6 Compare value of register a to memory at 0x1000 ..\n" +
                "                 set PC, end                      ;7 .. and jump to end if they don't match\n" +
                "\n" +
                "             set i, 0                             ;9 Init loop counter, for clarity\n" +
                ":nextchar    ife [data+i], 0                      ;10 If the character is 0 ..\n" +
                "                 set PC, end                      ;11 .. jump to the end\n" +
                "             set [0x8000+i], [data+i]             ;12 Video ram starts at 0x8000, copy char there\n" +
                "             add i, 1                             ;13 Increase loop counter\n" +
                "             set PC, nextchar                     ;14 Loop\n" +
                "  \n" +
                ":data        dat \"Hello_world!\", 0                ;16 Zero terminated string\n" +
                "\n" +
                ":end         hlt");
        printBytecode(bytecode);
        NotchDcpu dcpu = new NotchDcpu();
        dcpu.upload(bytecode);
        dcpu.run();
        System.out.println("Video memory:");
        int v = 0x8000;
        while (dcpu.mem[v] > 0) {
            System.out.print((char) dcpu.mem[v]);
            v++;
        }
        ///////////////////////////////////////////////////////////////////////////////////
        bytecode = compiler.compile("; Reading characters from the keyboard\n" +
                "; by Markus Persson\n" +
                "\n" +
                "#macro push(x){\n" +
                "\tset push,x\n" +
                "}\n" +
                "#macro pop(x)\n" +
                "\tset x,pop\n" +
                "\n" +
                "#macro nextkey(target) {\n" +
                "\tpush(i)\n" +
                "\tset i,[keypointer]\n" +
                "\tadd i,0x9000\n" +
                "\tset target,[i]\n" +
                "\tife target,0\n" +
                "\t\tjmp end\n" +
                "\t\n" +
                "\tset [i],0\n" +
                "\tadd [keypointer], 1\n" +
                "\tand [keypointer], 0xf\n" +
                ":end\n" +
                "\tpop(i)\n" +
                "}\n" +
                "nextkey(4)\n" +
                "\n" +
                ":keypointer\n" +
                "dat 0");/**/
        printBytecode(bytecode);
    }

    private static void printBytecode(short[] bytecode) {
        for (short i : bytecode) {
            System.out.printf("%04x ", i & 0xffff);
        }
        System.out.println();
    }
}
