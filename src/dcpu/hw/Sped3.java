package dcpu.hw;

import dcpu.Dcpu;

import java.awt.*;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 08.07.13
 * Time: 18:15
 */
public class Sped3 extends Dcpu.Device {
    public static final int HARDWARE_ID = 0x42babf3c;
    public static final char HARDWARE_VERSION = 0x0003;
    public static final int MANUFACTURER_ID = 0x1eb37e91;

    public static final int SPED_FREQUENCY = 50;

    public static final char OP_POLL = 0;
    public static final char OP_MAP = 1;
    public static final char OP_ROTATE = 2;

    public static final char STATE_NO_DATA = 0x0000;
    public static final char STATE_RUNNING = 0x0001;
    public static final char STATE_TURNING = 0x0002;

    public static final char ERROR_NONE = 0x0000;
    public static final char ERROR_BROKEN = 0xffff;

    public static final int X_MASK = 0x000000ff;
    public static final int Y_MASK = 0x0000ff00;
    public static final int Z_MASK = 0x00ff0000;
    public static final int C_MASK = 0x03000000;
    public static final int I_MASK = 0x04000000;
    public static final int IC_MASK = C_MASK | I_MASK;
    public static final int X_SHIFT = 0;
    public static final int Y_SHIFT = 8;
    public static final int Z_SHIFT = 16;
    public static final int C_SHIFT = 24;
    public static final int I_SHIFT = 26;
    public static final int IC_SHIFT = C_SHIFT;

    private int vertexCount = 0;
    private int vertexOffset = 0;
    private int angle = 0;
    private int tgtAngle = angle;
    private int da = 0;
    private boolean broken = false;
    private long nextTicks = 0;
    private Color[] colors = new Color[]{
            Color.decode("0x202020"),//I=0 C=00
            Color.decode("0x802020"),//I=0 C=01
            Color.decode("0x208020"),//I=0 C=10
            Color.decode("0x202080"),//I=0 C=11
            Color.decode("0x404040"),//I=1 C=00
            Color.decode("0xff4040"),//I=1 C=01
            Color.decode("0x40ff40"),//I=1 C=10
            Color.decode("0x4040ff") //I=1 C=11
    };

    public void breakDevice() {
        broken = true;
    }

    public void repairDevice() {
        broken = false;
        vertexCount = 0;
        vertexOffset = 0;
        angle = tgtAngle = 0;
    }

    @Override
    public int getHardwareId() {
        return HARDWARE_ID;
    }

    @Override
    public char getHardwareVersion() {
        return HARDWARE_VERSION;
    }

    @Override
    public int getManufacturerId() {
        return MANUFACTURER_ID;
    }

    @Override
    public void interrupt() {
        switch (cpu.getreg(Dcpu.Reg.A)) {
            case OP_POLL:
                if (vertexCount == 0) {
                    cpu.setreg(Dcpu.Reg.B, STATE_NO_DATA);
                } else if (da == 0) {
                    cpu.setreg(Dcpu.Reg.B, STATE_RUNNING);
                } else {
                    cpu.setreg(Dcpu.Reg.B, STATE_TURNING);
                }
                cpu.setreg(Dcpu.Reg.C, broken ? ERROR_BROKEN : ERROR_NONE);
                break;
            case OP_MAP:
                vertexOffset = cpu.getreg(Dcpu.Reg.X);
                vertexCount = cpu.getreg(Dcpu.Reg.Y);
                break;
            case OP_ROTATE:
                tgtAngle = cpu.getreg(Dcpu.Reg.X) % 360;
                if ((tgtAngle - angle + 360) % 360 > 180) {
                    da = 1;
                } else {
                    da = -1;
                }
                nextTicks = cpu.cycles + cpu.getFrequency() / SPED_FREQUENCY;
                break;
        }
    }

    @Override
    public void tick() {
        if (!broken && da != 0) {
            if (cpu.cycles > nextTicks) {
                angle = (angle + da + 360) % 360;
                nextTicks += cpu.getFrequency() / SPED_FREQUENCY;
                if (angle == tgtAngle) da = 0;
            }
        }
    }

    public void renderTo(IVertexRenderer renderer) {
        renderer.reset();
        int offset = vertexOffset;
        double cos = Math.cos(Math.toRadians(angle));
        double sin = Math.sin(Math.toRadians(angle));
        for (int i = 0; i < vertexCount; i++) {
            int dword;
            if (!broken) {
                dword = cpu.memget(offset) | (cpu.memget(offset + 1) << 16);
            } else {
                dword = new Random().nextInt();
            }
            double x0 = ((dword & X_MASK) >> X_SHIFT) / 256.0 - 0.5;
            double y0 = ((dword & Y_MASK) >> Y_SHIFT) / 256.0 - 0.5;
            double z0 = ((dword & Z_MASK) >> Z_SHIFT) / 256.0 - 0.5;
            double x = x0 * cos + y0 * sin;
            double y = -x0 * sin + y0 * cos;
            double z = z0;
            int ic = (dword & IC_MASK) >> IC_SHIFT;
            renderer.drawVertex(x, y, z, colors[ic]);
            offset += 2;
        }
    }
}
