package dcpu.demos;

import java.io.IOException;

import computer.AWTKeyMapping;
import computer.VirtualKeyboard;
import computer.VirtualMonitor;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.io.PanelPeripheral;

public class HelloWorldPanel {

    private Dcpu dcpu;

    public static void main(String[] args) throws IOException {
        new HelloWorldPanel().startDemo();
    }

    private void startDemo() throws IOException {
        dcpu = new Dcpu();
        PanelPeripheral panelPeripheral = new PanelPeripheral(new VirtualMonitor(dcpu.mem, 0x8000), new VirtualKeyboard(dcpu.mem, 0x9000, new AWTKeyMapping()));
        dcpu.attach(panelPeripheral, -1); // don't care about the line, just want it to tick
        
        dcpu.upload(new Assembler().assemble(
        		":mainloop" +
                "            ife [message + I], 0" +
        		"                set pc, end" +
                "            set a, [message + I]" +
        		"            add a, 0xA100" +
                "            set [0x8000 + I], a" +
        		"            add i, 1" +
                "            set pc, mainloop" +
        		":message    dat \"Hello, world!\", 0" +
                ":end        set pc, end"
        ));
        // dcpu.upload(HelloWorldPanel.class.getResourceAsStream("mem.dmp"));
        
        dcpu.reset();
        dcpu.run();
    }

}
