package dcpu.hw;

import dcpu.Dcpu;

public class SpedWindow extends DeviceWindow {
    private Sped3 sped3;

    public SpedWindow(Dcpu cpu, Sped3 sped3, boolean exitOnClose) {
        super(cpu, new SpedCanvas(), exitOnClose);
        ((SpedCanvas) canvas).setSped(sped3);
    }
}
