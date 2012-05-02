package dcpu.demos;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
        VirtualMonitor display = new VirtualMonitor(dcpu.mem, 0x8000);
        VirtualKeyboard keyboard = new VirtualKeyboard(dcpu.mem, 0x9000, new AWTKeyMapping());
        PanelPeripheral panelPeripheral = new PanelPeripheral(display, keyboard);
        
        panelPeripheral.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                dcpu.halt = true;
            }
        });
        dcpu.attach(panelPeripheral, -1); // don't care about the line, just want it to render the screen from cpu memory
        
        dcpu.upload(new Assembler().assemble(
                "            set a, 1\n" +
                "            add a, 1\n" +
                "            ife a, 2\n" +
                "                set a, 3\n" +
        		":mainloop\n" +
                "            ife [message + I], 0\n" +
        		"                set pc, end\n" +
                "            set a, [message + I]\n" +
        		"            add a, 0xA100\n" +
                "            set [0x8000 + I], a\n" +
        		"            add i, 1\n" +
                "            set pc, mainloop\n" +
        		":message    dat \"Hello, world!\", 0\n" +
                ":end        set pc, end\n"
        ));
        
        dcpu.reset();
        dcpu.run();
    }

}
