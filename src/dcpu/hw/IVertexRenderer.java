package dcpu.hw;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 08.07.13
 * Time: 18:32
 */
public interface IVertexRenderer {
    public void reset();

    public void drawVertex(double x, double y, double z, Color color);
}
