package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 03.04.12
 * Time: 0:59
 */
public class AssemblerDemo {
    public static void main(String[] args) {
        Assembler assembler = new Assembler();
        assembler.genMap = true;
        //////////////////////////////////
        // Hello World test
        System.out.println("\nHello World demo\n");
        char[] bytecode = assembler.assemble(";1 Assembler test for DCPU\n" +
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
        Dcpu dcpu = new Dcpu();
        dcpu.upload(bytecode);
        dcpu.run();
        System.out.println("Video memory:");
        int v = 0x8000;
        while (dcpu.mem[v] > 0) {
            System.out.print(dcpu.mem[v]);
            v++;
        }
        System.out.println();
        ///////////////////////////////////////////////
        //// Example from 1.1 docs
        System.out.println("\n1.1 demo\n");
        bytecode = assembler.assemble("        ; Try some basic stuff\n" +
                "                      SET A, 0x30              ; 7c01 0030\n" +
                "                      SET [0x1000], 0x20       ; 7de1 1000 0020\n" +
                "                      SUB A, [0x1000]          ; 7803 1000\n" +
                "                      IFN A, 0x10              ; c00d \n" +
                "                         SET PC, crash         ; 7dc1 001a [*]\n" +
                "                      \n" +
                "        ; Do a loopy thing\n" +
                "                      SET I, 10                ; a861\n" +
                "                      SET A, 0x2000            ; 7c01 2000\n" +
                "        :loop         SET [0x2000+I], [A]      ; 2161 2000\n" +
                "                      SUB I, 1                 ; 8463\n" +
                "                      IFN I, 0                 ; 806d\n" +
                "                         SET PC, loop          ; 7dc1 000d [*]\n" +
                "        \n" +
                "        ; Call a subroutine\n" +
                "                      SET X, 0x4               ; 9031\n" +
                "                      JSR testsub              ; 7c10 0018 [*]\n" +
                "                      SET PC, crash            ; 7dc1 001a [*]\n" +
                "        \n" +
                "        :testsub      SHL X, 4                 ; 9037\n" +
                "                      SET PC, POP              ; 61c1\n" +
                "                        \n" +
                "        ; Stop DCPU.\n" +
                "        :crash        HLT            ; 0000 0000\n" +
                "        ");
        dcpu.reset();
        dcpu.upload(bytecode);
        dcpu.run();
        System.out.printf("X = %04x\n", (int) dcpu.mem[Dcpu.M_X]);

        ///////////////////////////////////////////////////////////////////////////////////
        // Macros - not supported yet
        System.out.println("\nMacro and keyboard demo\n");
        bytecode = assembler.assemble("; Reading characters from the keyboard\n" +
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

    private static void printBytecode(char[] bytecode) {
        for (char i : bytecode) {
            System.out.printf("%04x ", i & 0xffff);
        }
        System.out.println();
    }
}
