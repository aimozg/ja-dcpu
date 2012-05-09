package dcpu.demos;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import computer.AWTKeyMapping;
import computer.VirtualKeyboard;
import computer.VirtualMonitor;

import dcpu.AntlrAssembler;
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
        
        dcpu.upload(new AntlrAssembler().assemble(DemoUtils.getDemoAsmReader(HelloWorldPanel.class)));
        
        dcpu.reset();
        dcpu.run();
    }

}
