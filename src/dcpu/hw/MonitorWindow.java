package dcpu.hw;

import dcpu.Dcpu;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Simple monitor window
 */
public class MonitorWindow {

    private JFrame frame;
    private final Dcpu cpu;
    private IMonitor monitor;
    private MonitorCanvas canvas;

    public MonitorWindow(Dcpu cpu, IMonitor monitor, boolean exitOnClose) {
        this.monitor = monitor;
        this.cpu = cpu;
        frame = new JFrame();
        frame.setDefaultCloseOperation(exitOnClose ? WindowConstants.EXIT_ON_CLOSE : WindowConstants.DO_NOTHING_ON_CLOSE);
        canvas = new MonitorCanvas();
        canvas.setMonitor(monitor, 4);
        canvas.setFocusable(true);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                MonitorWindow.this.cpu.halt = true;
            }
        });
    }

    public void addKeyListener(KeyListener listener) {
        canvas.addKeyListener(listener);
    }

    public void show() {
        frame.setVisible(true);
        canvas.requestFocus();
        // TODO maybe move thread to MonitorCanvas?
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

    public void close() {
        frame.setVisible(false);
        frame.dispose();
    }
}
