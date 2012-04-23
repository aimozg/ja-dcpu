package dcpu.io;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;

import computer.VirtualKeyboard;
import computer.VirtualMonitor;

import dcpu.Dcpu.Peripheral;

public class PanelPeripheral extends Peripheral {
    private static final int SCALE = 3;
    private BufferedImage borderedWindow;
    private Canvas canvas;
    private BufferedImage mainWindow;
    private final VirtualMonitor display;
    private final VirtualKeyboard keyboard;
    private boolean runThread = true;
    private JFrame frame;
    private Thread renderThread;

    public PanelPeripheral(VirtualMonitor display, VirtualKeyboard keyboard) {
        this.display = display;
        this.keyboard = keyboard;
        createPanel();
    }

    private void createPanel() {
        frame = new JFrame();
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(160 * SCALE, 128 * SCALE));
        canvas.setMinimumSize(new Dimension(160 * SCALE, 128 * SCALE));
        canvas.setMaximumSize(new Dimension(160 * SCALE, 128 * SCALE));
        canvas.setFocusable(true);

        canvas.addKeyListener(new KeyListener() {
            @Override public void keyPressed(KeyEvent ke) {
                keyboard.keyPressed(ke.getKeyCode());
            }
            @Override public void keyReleased(KeyEvent ke) {
                keyboard.keyReleased(ke.getKeyCode());
            }
            @Override public void keyTyped(KeyEvent ke) {
                keyboard.keyTyped(ke.getKeyChar());
            }
        });

        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                // tell thread to stop, wait for it then dispose of frame
                runThread = false;
                try { renderThread.join(); } catch (InterruptedException e) {}
                frame.dispose();
            }
        });
        
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setVisible(true);
        
        borderedWindow = new BufferedImage(160, 128, 2);
        mainWindow = new BufferedImage(128, 128, 2);
        display.setPixels(((DataBufferInt) mainWindow.getRaster().getDataBuffer()).getData());
        canvas.requestFocus();
        createPanelThread();
    }

    private void createPanelThread() {
    	renderThread = new Thread() {
    		@Override public void run() {
    			do {
    				renderPanel();
    				try { Thread.sleep(1L); } catch (InterruptedException e) {}
    			} while (runThread);
    			
    		}
    	};
    	renderThread.start();
	}
    
    public void killPanel() {
    	WindowEvent wev = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
    	Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
    }

    public void renderPanel() {
        display.render();
        Graphics g = borderedWindow.getGraphics();
        g.setColor(new Color(display.getBackgroundColor()));
        g.fillRect(0, 0, 160, 128);
        g.drawImage(mainWindow, 16, 16, 128, 128, null);
        g.dispose();
        g = canvas.getGraphics();
        g.drawImage(borderedWindow, 0, 0, 160 * SCALE, 128 * SCALE, null);
        g.dispose();
    }
    
    public void addWindowListener(WindowListener listener) {
        frame.addWindowListener(listener);
    }
    
}
