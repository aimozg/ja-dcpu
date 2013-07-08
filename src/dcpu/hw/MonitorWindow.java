package dcpu.hw;

import dcpu.Dcpu;

/**
 * Simple monitor window
 */
public class MonitorWindow extends DeviceWindow {

    private static MonitorCanvas mkcanvas(IMonitor monitor) {
        MonitorCanvas mc = new MonitorCanvas();
        mc.setMonitor(monitor, 4);
        return mc;
    }

    public MonitorWindow(Dcpu cpu, IMonitor monitor, boolean exitOnClose) {
        super(cpu, mkcanvas(monitor), exitOnClose);
        frame.setResizable(false);
    }

}

