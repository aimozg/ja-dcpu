package dcpu.hw;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class MonitorCanvas extends JComponent {

    private BufferedImage screen;
    private int[] screenPixels;
    private int scale;
    private int borderSize = 16;
    private IMonitor monitor;
    private int sbwidth;//screen buffer width
    private int sbheight;//screen buffer height

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(sbwidth + 2 * borderSize, sbheight + 2 * borderSize);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public void setMonitor(IMonitor monitor, int scale) {
        this.monitor = monitor;
        setScale(monitor, scale);
        screen = new BufferedImage(monitor.getWidth(), monitor.getHeight(), BufferedImage.TYPE_INT_RGB);
        screenPixels = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();
    }

    public void setScale(IMonitor monitor, int scale) {
        this.scale = scale;
        sbwidth = monitor.getWidth() * scale;
        sbheight = monitor.getHeight() * scale;
        invalidate();
    }

    @Override
    public void paint(Graphics g) {
        if (monitor == null) return;
        g.setColor(new Color(monitor.getBorderColorRGB()));
        g.fillRect(0, 0, borderSize * 2 + sbwidth, borderSize * 2 + sbheight);
        monitor.render(screenPixels);
        g.drawImage(screen, borderSize, borderSize, sbwidth, sbheight, null);
    }
}
