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
        
        cpu.upload(new Assembler().assemble(DemoUtils.getDemoAsmReader(DeviceDemo.class)));

        MonitorWindow monitorWindow = new MonitorWindow(cpu, monitor, true);
        monitorWindow.addKeyListener(keyboard);
        monitorWindow.show();

        cpu.run();
    }
}
