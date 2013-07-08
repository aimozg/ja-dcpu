package dcpu.hw;

import javax.swing.*;
import java.awt.*;

public class SpedCanvas extends JComponent {
    private Sped3 sped;
    private double planeWidth = 2;
    private double planeHeight = 2;
    private double planeDepth = 2;
    private double cameraDist = -2;

    /**
     * @param planeWidth  render plane width
     * @param planeHeight render plane height
     * @param planeDepth  distance from camera to render plane
     * @param cameraZ     distance from camera to (0,0,0)
     */
    public void setProjection(double planeWidth, double planeHeight, double planeDepth, double cameraZ) {
        this.planeWidth = planeWidth;
        this.planeHeight = planeHeight;
        this.planeDepth = planeDepth;
        this.cameraDist = cameraZ;
    }

    public void setSped(Sped3 sped) {
        this.sped = sped;
    }

    @Override
    public void paint(final Graphics g) {
        final int pixw = getSize().width;
        final int pixh = getSize().height;
        sped.renderTo(new IVertexRenderer() {

            boolean first = true;
            int prevx
                    ,
                    prevy;

            @Override
            public void reset() {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, pixw, pixh);
            }

            @Override
            public void drawVertex(double x, double y, double z, Color color) {
                // y is right, z is down, x is forward
                x -= cameraDist;
                // project to plane
                double xp = y * planeDepth / x;
                double yp = z * planeDepth / x;
                // scale from [-pW/2 pW/2] to [0 1]
                double x1 = xp / planeWidth + 0.5;
                double y1 = yp / planeHeight + 0.5;
                // transform onto canvas coordinates
                int px = (int) (x1 * pixw);
                int py = (int) (y1 * pixh);
                if (!first) {
                    g.setColor(color);
                    g.drawLine(prevx, prevy, px, py);
                }
                first = false;
                prevx = px;
                prevy = py;
            }
        });
    }
}
