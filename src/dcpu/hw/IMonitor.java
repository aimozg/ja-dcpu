package dcpu.hw;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 02.05.12
 * Time: 23:52
 */
public interface IMonitor {
    int getHeight();

    int getWidth();

    int getBorderColorRGB();

    void render(int[] pixels);
}
