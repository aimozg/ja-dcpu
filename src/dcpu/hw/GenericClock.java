package dcpu.hw;

import dcpu.Dcpu;

public class GenericClock extends Dcpu.Device {

    public static final int HARDWARE_ID = 0x12d0b402;

    public static final short CLKINT_SETUP = 0;
    public static final short CLKINT_GETTICKS = 1;
    public static final short CLKINT_TOGGLEINT = 2;

    private final short manufacturerId;

    public GenericClock(short manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    @Override
    public int getHardwareId() {
        return HARDWARE_ID;
    }

    @Override
    public short getHardwareVersion() {
        return 1;
    }

    @Override
    public short getManufacturerId() {
        return manufacturerId;
    }

    protected long timerTicks = 0; // number of timer ticks (INT 1)
    protected long lastShot = 0; // cpu cycles from last timer shot
    protected int period = 0;
    protected short intMsg = 0;

    // Same as Dcpu.CYCLES_PER_FRAME, but don't want to rely on that
    protected static final long CYCLES_PER_SHOT = Dcpu.CPU_FREQUENCY / 60;

    @Override
    public void tick() {
        if (period == 0) return;
        if (cpu.totalCycles - lastShot > period * CYCLES_PER_SHOT) {
            lastShot += period * CYCLES_PER_SHOT;
            timerTicks++;
            if (intMsg != 0) cpu.interrupt(intMsg);
        }
    }

    @Override
    public void interrupt() {
        switch (cpu.getreg(Dcpu.Reg.A)) {
            case CLKINT_SETUP:
                period = cpu.getreg(Dcpu.Reg.B) & 0xffff;
                lastShot = cpu.totalCycles;
                timerTicks = 0;
                break;
            case CLKINT_GETTICKS:
                cpu.setreg(Dcpu.Reg.B, (short) timerTicks);
                break;
            case CLKINT_TOGGLEINT:
                intMsg = cpu.getreg(Dcpu.Reg.B);
                break;
        }
    }
}
