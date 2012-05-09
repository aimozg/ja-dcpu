package dcpu.demos;

import dcpu.AntlrAssembler;
import dcpu.Dcpu;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 03.04.12
 * Time: 0:59
 */
public class AssemblerDemo {
    public static void main(String[] args) {
        AntlrAssembler assembler = new AntlrAssembler();
        assembler.setGenerateMap(true);
        //////////////////////////////////
        // Hello World test
        System.out.println("\nHello World demo\n");
        char[] bytecode = assembler.assemble(DemoUtils.getDemoAsmReader(AssemblerDemo.class, "_1"));
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
        bytecode = assembler.assemble(DemoUtils.getDemoAsmReader(AssemblerDemo.class, "_2"));
        dcpu.reset();
        dcpu.upload(bytecode);
        dcpu.run();
        System.out.printf("X = %04x\n", (int) dcpu.mem[Dcpu.M_X]);

        ///////////////////////////////////////////////////////////////////////////////////
        // Macros - not supported yet
        System.out.println("\nMacro and keyboard demo\n");
        bytecode = assembler.assemble(DemoUtils.getDemoAsmReader(AssemblerDemo.class, "_3"));/**/
        printBytecode(bytecode);
    }

    private static void printBytecode(char[] bytecode) {
        for (char i : bytecode) {
            System.out.printf("%04x ", (int) i);
        }
        System.out.println();
    }
}
