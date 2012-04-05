package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.Tracer;
import dcpu.io.InstreamPeripheral;
import dcpu.io.OutstreamPeripheral;

import java.io.ByteArrayOutputStream;

/**
 * Asks user's name and greets him.
 */
public class StdinoutDemo {
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();
        Assembler assembler = new Assembler();
        cpu.upload(assembler.assemble(
                ":main\n" +
                        "     SET A,0x8000 ; stdout\n" +
                        "     SET B,0x9000 ; stdin\n" +
                        "     ; println(greet1)\n" +
                        "     SET I,greet1\n" +
                        "     JSR println\n" +
                        "     ; readln(name)\n" +
                        "     SET I,name\n" +
                        "     JSR readln\n" +
                        "     ; append zero to name\n" +
                        "     SET [I],0\n" +
                        "     ; println(greet2)\n" +
                        "     SET I,greet2\n" +
                        "     JSR println\n" +
                        "     ; println(name)\n" +
                        "     SET I,name\n" +
                        "     JSR println\n" +
                        "     ; println(greet3)\n" +
                        "     SET I,greet3\n" +
                        "     JSR println\n" +
                        "     ; goto end\n" +
                        "     SET PC,end\n" +
                        "\n" +
                        "\n" +
                        "; println()\n" +
                        ";        prints zero-terminated line to output\n" +
                        "; Input:\n" +
                        ";       [I],[I+1],... - zero-terminated line\n" +
                        ";       [A] - output peripheral\n" +
                        "; Output:\n" +
                        ";        I points to end of line\n" +
                        ":println\n" +
                        "        ; while [I]\n" +
                        "        IFE [I],0\n" +
                        "            SET PC,POP\n" +
                        "        ; [A] = [I++]\n" +
                        "        SET [A],[I]\n" +
                        "        ADD I,1\n" +
                        "        SET PC,println\n" +
                        "\n" +
                        "; readln()\n" +
                        ";      reads \\n-terminated line from input\n" +
                        "; Input:\n" +
                        ";       I - pointer to input buffer\n" +
                        ";       [B] - input peripheral\n" +
                        "; Output:\n" +
                        ";        I - pointer to last character of input (which is \\n)\n" +
                        "\n" +
                        ":readln\n" +
                        "       SET [I],[B]\n" +
                        "       IFE [I],0xffff ; if nothing on input yet, repeat\n" +
                        "           SET PC,readln\n" +
                        "       ; if [I] == \\n, return.\n" +
                        "       IFE [I],0x0a\n" +
                        "           SET PC,POP\n" +
                        "       ADD I,1\n" +
                        "       SET PC,readln\n" +
                        "\n" +
                        ":greet1\n" +
                        "       dat \"Hello, what's your name?\",0xd,0xa,0\n" +
                        ":greet2\n" +
                        "       dat \"Hello, \",0\n" +
                        ":greet3\n" +
                        "       dat \"!\",0xd,0xa,0\n" +
                        ":end\n" +
                        "    dat 0\n" +
                        ":name"
        ));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutstreamPeripheral stdout = new OutstreamPeripheral(byteArrayOutputStream);
        cpu.attach(stdout, 0x8);
        InstreamPeripheral stdin = new InstreamPeripheral(System.in, 100);
        cpu.attach(stdin, 0x9);

        Tracer tracer = new Tracer();
        tracer.printRegisters(true);
        tracer.printMemAtReg(true);
        tracer.printStack(4);
        //tracer.install(cpu);

        cpu.reset();
        cpu.run();

    }
}
