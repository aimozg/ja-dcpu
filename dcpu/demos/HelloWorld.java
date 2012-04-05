package dcpu.demos;

import dcpu.Dcpu;

import static dcpu.Dcpu.*;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 28.03.12
 * Time: 23:30
 */
public class HelloWorld {
    /**
     * First demo program that copies "Hello_world!" to video memory.
     * <p/>
     * Video memory starts at 0x8000 and is buffer of 80x25 characters. High bytes ignored
     */
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();

        ///////////
        // LOAD PROGRAM
        short end = (short) 0xffff;
        short data = (short) 0x2000;
        short video = (short) 0x8000;
        int i_video = video & 0xffff;
        int i_data = data & 0xffff;

        //set a, 0xbeef
        cpu.mem[0] = gencmd(O_SET, A_A, A_NW);
        cpu.mem[1] = (short) 0xbeef;
        // set (0x1000), a
        cpu.mem[2] = gencmd(O_SET, A_M_NW, A_A);
        cpu.mem[3] = (short) 0x1000;
        // ifn a, (0x1000)
        cpu.mem[4] = gencmd(O_IFN, A_A, A_M_NW);
        cpu.mem[5] = (short) 0x1000;
        // set PC, end
        cpu.mem[6] = gencmd(O_SET, A_PC, A_NW);
        cpu.mem[7] = end;
        // set i, 0
        cpu.mem[8] = gencmd(O_SET, A_I, A_0);
        //:nextchar
        // ife (data+i), 0
        cpu.mem[9] = gencmd(O_IFE, A_M_NW_I, A_0);
        cpu.mem[10] = data;
        // set PC, end
        cpu.mem[11] = gencmd(O_SET, A_PC, A_NW);
        cpu.mem[12] = end;
        // set (video+i), (data+i)
        cpu.mem[13] = gencmd(O_SET, A_M_NW_I, A_M_NW_I);
        cpu.mem[14] = video;
        cpu.mem[15] = data;
        // add i, 1
        cpu.mem[16] = gencmd(O_ADD, A_I, A_1);
        // set PC, nextchar
        cpu.mem[17] = gencmd(O_SET, A_PC, A_9);

        // end
        cpu.mem[0xffff] = gencmd_nbi(O__RESVD, 0);

        int i = i_data;
        for (char c : "Hello_world!".toCharArray()) { // copy to data
            cpu.mem[i] = (short) c;
            i++;
        }
        cpu.mem[i] = 0;

        ///////////
        // LAUNCH
        cpu.reset();
        cpu.run();
        /*while (!cpu.reserved){ // until we hit operation 0
            //System.out.println(cpu._dregs());
            cpu.step(false);
        }*/
        System.out.println("\n\nExecution completed\n\n");
        /////////
        // OUTPUT VIDEO MEMORY
        System.out.println("Video memory:");
        for (i = 0; i < 82; i++) System.out.print("#");
        System.out.println();
        for (i = 0; i < 25; i++) {
            System.out.print("#");
            for (int j = 0; j < 80; j++) {
                short m = cpu.mem[i_video + i * 80 + j];
                System.out.print((m >= 0x20 && m <= 0x80) ? (char) m : ' ');
            }
            System.out.println("#");
        }
        for (i = 0; i < 82; i++) System.out.print("#");
        System.out.println();
    }
}
