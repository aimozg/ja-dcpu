package dcpu.hw;

import dcpu.Dcpu;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DeviceWindow {

    protected final Dcpu cpu;
    protected JFrame frame;
    protected JComponent canvas;

    DeviceWindow(Dcpu cpu, JComponent canvas, boolean exitOnClose) {
        this.cpu = cpu;
        this.canvas = canvas;
        frame = new JFrame();
        canvas.setFocusable(true);
        frame.setDefaultCloseOperation(exitOnClose ? WindowConstants.EXIT_ON_CLOSE : WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DeviceWindow.this.cpu.halt = true;
            }
        });

    }

    public void addKeyListener(KeyListener listener) {
        canvas.addKeyListener(listener);
    }

    public void show() {
        frame.setVisible(true);
        canvas.requestFocus();
        // TODO maybe move thread to MonitorCanvas/SpedCanvas?
        Thread renderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (frame.isVisible()) {
                    canvas.repaint();
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        });
        renderThread.setDaemon(true);
        renderThread.start();
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComponent getCanvas() {
        return canvas;
    }

    public void close() {
        frame.setVisible(false);
        frame.dispose();
    }
}
