package dcpu.demos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import computer.AWTKeyMapping;
import computer.VirtualKeyboard;
import computer.VirtualMonitor;

import dcpu.Dcpu;
import dcpu.io.PanelPeripheral;

public class HighNerd {

    private Dcpu dcpu;

    public static void main(String[] args) throws IOException {
        new HighNerd().startDemo();
    }

    private void startDemo() throws IOException {
        dcpu = new Dcpu();
        VirtualMonitor display = new VirtualMonitor(dcpu.mem, 0x8000);
        VirtualKeyboard keyboard = new VirtualKeyboard(dcpu.mem, 0x9000, new AWTKeyMapping());
        PanelPeripheral panelPeripheral = new PanelPeripheral(display, keyboard);
        dcpu.attach(panelPeripheral, -1);
        dcpu.upload(getShortsFromResource("mem.dmp"));
        dcpu.reset();
        dcpu.run();
    }

    private short[] getShortsFromResource(String resourceName) throws IOException {
        int nRead;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(2048);
        byte[] data = new byte[0x4000];
        InputStream stream = HighNerd.class.getResourceAsStream(resourceName);
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        ByteBuffer bb = ByteBuffer.wrap(buffer.toByteArray());
        ShortBuffer sb = bb.asShortBuffer();
        short[] shorts = new short[sb.limit()];
        sb.get(shorts);
        return shorts;
    }

}
