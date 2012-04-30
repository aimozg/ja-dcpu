package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.hw.IostreamDevice;

/**
 * Asks user's name and greets him.
 * <p/>
 * Updated to use IostreamDevice
 */
public class StdinoutDemo {
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();
        Assembler assembler = new Assembler();
        cpu.upload(assembler.assemble(":main\n" +
                "    ; locate stdio\n" +
                "    HWN Z\n" +
                ":nexthw\n" +
                "    IFE Z,0\n" +
                "        SET PC,endhw\n" +
                "    SUB Z,1\n" +
                "    HWQ Z\n" +
                "    IFE B,0x494f ;'IO'\n" +
                "        IFE A,0x5344 ;'SD'\n" +
                "            SET [stdio_hw],Z\n" +
                "    SET PC,nexthw\n" +
                "    \n" +
                ":endhw\n" +
                "    ; println(greet1)\n" +
                "    SET B,greet1\n" +
                "    JSR println\n" +
                "    ; readln(name)\n" +
                "    SET I,name\n" +
                "    JSR readln\n" +
                "    ; append zero to name\n" +
                "    SET [I],0\n" +
                "    ; println(greet2)\n" +
                "    SET B,greet2\n" +
                "    JSR println\n" +
                "    ; println(name)\n" +
                "    SET B,name\n" +
                "    JSR println\n" +
                "    ; println(greet3)\n" +
                "    SET B,greet3\n" +
                "    JSR println\n" +
                "    ; goto end\n" +
                "    SET PC,end\n" +
                "\n" +
                "\n" +
                "; println()\n" +
                ";        prints zero-terminated line to output\n" +
                "; Input:\n" +
                ";       [B],[B+1],... - zero-terminated line\n" +
                ":println\n" +
                "    SET PUSH,A\n" +
                "    SET A,2\n" +
                "    HWI [stdio_hw]\n" +
                "    SET A,POP\n" +
                "    SET PC,POP\n" +
                "\n" +
                "; readln()\n" +
                ";      reads \\n-terminated line from input (without \\n)\n" +
                "; Input:\n" +
                ";       I - pointer to input buffer\n" +
                "; Output:\n" +
                ";       I - pointer beyond last character of input\n" +
                "\n" +
                ":readln\n" +
                "    SET PUSH,A\n" +
                "    SET PUSH,J\n" +
                "    SET A,1\n" +
                "    :readln_getc\n" +
                "        HWI [stdio_hw]\n" +
                "        IFE B,0xffff ; if nothing on input yet, repeat\n" +
                "            SET PC,readln_getc\n" +
                "        IFE B,0x0a\n" +
                "            SET PC,readln_end\n" +
                "        STI [I],B\n" +
                "        SET PC,readln_getc\n" +
                ":readln_end\n" +
                "    SET J,POP\n" +
                "    SET A,POP\n" +
                "    SET PC,POP\n" +
                "\n" +
                ":stdio_hw\n" +
                "    dat 0\n" +
                ":greet1\n" +
                "    dat \"Hello, what's your name?\",0xd,0xa,0\n" +
                ":greet2\n" +
                "    dat \"Hello, \",0\n" +
                ":greet3\n" +
                "    dat \"!\",0xd,0xa,0\n" +
                ":end\n" +
                "    hcf 0\n" +
                ":name"
        ));

        IostreamDevice stdio = new IostreamDevice(System.in, System.out);
        cpu.attach(stdio);

        cpu.reset();
        cpu.run();

    }
}
