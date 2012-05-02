package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.hw.MonitorLEM1802;
import dcpu.hw.MonitorWindow;

public class HelloWorldMonitor {
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();
        MonitorLEM1802 monitor = new MonitorLEM1802();
        cpu.attach(monitor);
        MonitorWindow window = new MonitorWindow(cpu, monitor, true);

        cpu.upload(new Assembler().assemble("" +
                // detect monitor
                "   HWN Z\n" +
                ":next_hw\n" +
                "   IFE Z,0\n" +
                "       SET PC,end_hw\n" +
                "   SUB Z,1\n" +
                "   HWQ Z\n" +
                "   IFE B,0x7349\n" +
                "       IFE A,0xf615\n" +
                "           SET [mon_hw],Z\n" +
                "   SET PC,next_hw\n" +
                ":end_hw\n" +
                // configure monitor: screenbuffer at 0x8000
                "   SET A,0\n" +
                "   SET B,0x8000\n" +
                "   HWI [mon_hw]\n" +
                // loop: display message
                "   SET I,message\n" +
                "   SET J,0x7fff\n" + // J is incremented before writing
                "   SET Z,0xf000\n" +
                ":loop\n" +
                "   STI A,[I]\n" +
                "   BOR A,Z\n" +
                "   SET [J],A\n" +
                "   IFE [I],0\n" + //end of msg - switch colors and start over
                "       ADD Z,0x0100\n" + // switch colors
                "   IFE [I],0\n" +
                "       SET I,message\n" +
                //"       HCF 0\n" +
                "   IFE J,0x817f\n" + // end of vram - start over
                "       SET J,0x7fff\n" +
                "   SET PC,loop\n" +
                "   HCF 0\n" +
                ":message\n" +
                "   dat \"Hello, World!!!!\",0\n" +
                ":mon_hw dat 0\n"//monitor hw#
        ));

        //new Tracer(System.out).install(cpu);

        window.show();
        cpu.run();
    }
}
