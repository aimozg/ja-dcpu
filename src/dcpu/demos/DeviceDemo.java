package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.hw.GenericClock;
import dcpu.hw.GenericKeyboard;
import dcpu.hw.MonitorLEM1802;
import dcpu.hw.MonitorWindow;

/**
 * Demo for testing display, clock, and keyboard
 * <p/>
 * Assembly by Jetarmors (http://0x10co.de/47btl), modified by aimozg
 */
public class DeviceDemo {
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();
        MonitorLEM1802 monitor = new MonitorLEM1802();
        GenericClock clock = new GenericClock(MonitorLEM1802.MANUFACTURER_ID);//Nya Elektriska
        GenericKeyboard keyboard = new GenericKeyboard(MonitorLEM1802.MANUFACTURER_ID, 16);
        cpu.attach(monitor);
        cpu.attach(clock);
        cpu.attach(keyboard);

        cpu.upload(new Assembler().assemble("" +
                ";initialize monitor, keyboard, clock\n" +
                "HWN A\n" +
                "SET Z, A\n" +
                ":device_set_loop\n" +
                "SUB Z, 1\n" +
                "HWQ Z\n" +
                "IFE A, 0xf615\n" +
                "  JSR monitor_setup\n" +
                "IFE A, 0x7406\n" +
                "  JSR keyboard_setup\n" +
                "IFE A, 0xb402\n" +
                "  JSR clock_setup\n" +
                "IFN Z, 0\n" +
                "  SET PC, device_set_loop\n" +
                "\n" +
                ";initialize console\n" +
                "SET C, 0\n" +
                "SET I, 0x8020\n" +
                "SET [I], [cursor]\n" +
                "SET B, I\n" +
                "SUB B, 1\n" +
                "SET X, I\n" +
                "ADD X, 0x15f\n" +
                "\n" +
                "; wait for input\n" +
                ":console_loop\n" +
                "SET A, 1\n" +
                "HWI [keyboard_address]\n" +
                "IFE C, 0         ;; no input\n" +
                "  SET PC, console_loop\n" +
                "IFE C, 8  ;; for some reason after going through backspace an HWI call will return 8?\n" +
                "  SET PC, console_loop\n" +
                "\n" +
                "IFE C, 0x11       ;; enter pressed\n" +
                "  SET PC, console_end\n" +
                "\n" +
                "IFE C, 0x10      ;; backspace pressed\n" +
                "  JSR backspace\n" +
                "\n" +
                "; echo input, move to next character\n" +
                "BOR C, 0x2000\n" +
                "IFN C, 0x2010\n" +
                "  SET [I], C\n" +
                "ADD I, 1\n" +
                "IFG I, X\n" +
                "  SUB I, 1\n" +
                "SET C, 0\n" +
                "SET [I], [cursor]\n" +
                "SET PC, console_loop\n" +
                "\n" +
                ":backspace            ;; self-explanatory\n" +
                "SET [I], 0x0\n" +
                "SUB I, 1\n" +
                "IFN I, B\n" +
                "  SET [I], 0x0\n" +
                "IFN I, B\n" +
                "  SUB I, 1\n" +
                "SET PC, POP\n" +
                "\n" +
                ":console_end\n" +
                "SET [I], 0x0\n        ;; remove cursor\n" +
                "SET PC, console_end\n" +
                "\n" +
                "\n" +
                "\n" +
                ":monitor_setup\n" +
                "SET [monitor_address],Z\n" +
                ";map screen to ram\n" +
                "SET A, 0\n" +
                "SET B, 0x8000\n" +
                "HWI Z\n" +
                ";initialize clock\n" +
                ":monitor_setup_clock\n" +
                "SET [B], 0xf030\n" +
                "ADD B, 1\n" +
                "IFN B, 0x8005\n" +
                "  SET PC, monitor_setup_clock\n" +
                "SET [0x8002], 0xf03A\n" +
                ";set background color\n" +
                "SET A, 3\n" +
                "SET B, 0x0\n" +
                "HWI Z\n" +
                "SET PC, POP\n" +
                "\n" +
                ":keyboard_setup\n" +
                "SET [keyboard_address], Z\n" +
                ";clear keyboard buffer\n" +
                "SET A, 0\n" +
                "HWI Z\n" +
                "SET PC, POP\n" +
                "\n" +
                ":clock_setup\n" +
                "SET [clock_address], Z\n" +
                ";tick once every second\n" +
                "SET A, 0\n" +
                "SET B, 60\n" +
                "HWI [clock_address]\n" +
                ";interrupt at every tick\n" +
                "IAS update_clock\n" +
                "SET A, 2\n" +
                "SET B, 0x8004\n" +
                "HWI [clock_address]\n" +
                "SET PC, POP\n" +
                "\n" +
                "\n" +
                "\n" +
                ":update_clock\n" +
                ";adds a second to the clock for every tick\n" +
                "IFE A, 0x8003\n" +
                "  IFE [A], 0xf035\n" +
                "  ADD PC, 3\n" +
                "IFN [A], 0xf039\n" +
                "  SET PC, clock_add\n" +
                "SET [A], 0xf030\n" +
                "SUB A, 1\n" +
                "IFE A, 0x8002\n" +
                "  SUB A, 1\n" +
                "IFN A, 0x7fff\n" +
                "  SET PC, update_clock\n" +
                "SET PC, end_clock_interrupt\n" +
                "\n" +
                ":clock_add\n" +
                "ADD [A], 1\n" +
                ":end_clock_interrupt\n" +
                "RFI 0\n" +
                "\n" +
                "\n" +
                "\n" +
                ":cursor\n" +
                "DAT 0x209c\n" +
                "\n" +
                ":keyboard_address\n" +
                "DAT 0\n" +
                "\n" +
                ":monitor_address\n" +
                "DAT 0\n" +
                "\n" +
                ":clock_address\n" +
                "DAT 0"
        ));

        MonitorWindow monitorWindow = new MonitorWindow(cpu, monitor, true);
        monitorWindow.addKeyListener(keyboard);
        monitorWindow.show();

        cpu.run();
    }
}
