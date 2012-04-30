package dcpu.demos;

import dcpu.Dcpu;
import dcpu.io.OutstreamPeripheral;

import static dcpu.Dcpu.*;

public class StdoutDemo {
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();
        OutstreamPeripheral stdout = new OutstreamPeripheral(System.out);
        cpu.attach(stdout, 0xe); // stdout gets writes to 0xe000-0xefff

        char data = 0x1000;
        char tgt = 0xe123;

        // :loop
        // ife (data+i),0
        //      hlt (reserved op)
        // set (tgt+i),(data+i)
        // add i,1
        // set pc,loop
        cpu.mem[0] = gencmd(O_IFE, A_M_NW_I, A_0);
        cpu.mem[1] = data;
        cpu.mem[2] = 0;
        cpu.mem[3] = gencmd(O_SET, A_M_NW_I, A_M_NW_I);
        cpu.mem[4] = tgt;
        cpu.mem[5] = data;
        cpu.mem[6] = gencmd(O_ADD, A_I, A_1);
        cpu.mem[7] = gencmd(O_SET, A_PC, 0);

        int i_tgt = data & 0xffff;
        for (char c : "Hi universe!".toCharArray()) {
            cpu.mem[i_tgt] = c;
            i_tgt++;
        }

        cpu.reset();
        cpu.run();
    }
}
