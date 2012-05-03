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

        cpu.upload(new Assembler().assemble(DemoUtils.getDemoAsmReader(HelloWorldMonitor.class)));

        window.show();
        cpu.run();
    }
}
