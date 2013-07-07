package dcpu.hw;

import dcpu.Dcpu;

public class GenericClock extends Dcpu.Device {

    public static final int HARDWARE_ID = 0x12d0b402;

    public static final char CLKINT_SETUP = 0;
    public static final char CLKINT_GETTICKS = 1;
    public static final char CLKINT_TOGGLEINT = 2;

    private final int manufacturerId;

    public GenericClock(int manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    @Override
    public int getHardwareId() {
        return HARDWARE_ID;
    }

    @Override
    public char getHardwareVersion() {
        return 1;
    }

    @Override
    public int getManufacturerId() {
        return manufacturerId;
    }

    protected long timerTicks = 0; // number of timer ticks (INT 1)
    protected long lastShot = 0; // cpu cycles from last timer shot
    protected int period = 0;
    protected char intMsg = 0;

    @Override
    public void tick() {
        if (period == 0) return;
        long cyclesPerShot = cpu.getFrequency() / Dcpu.FRAMES_PER_SECOND;
        if (cpu.cycles - lastShot > period * cyclesPerShot) {
            lastShot += period * cyclesPerShot;
            timerTicks++;
            if (intMsg != 0) cpu.interrupt(intMsg);
        }
    }

    @Override
    public void interrupt() {
        switch (cpu.getreg(Dcpu.Reg.A)) {
            case CLKINT_SETUP:
                period = cpu.getreg(Dcpu.Reg.B);
                lastShot = cpu.cycles;
                timerTicks = 0;
                break;
            case CLKINT_GETTICKS:
                cpu.setreg(Dcpu.Reg.B, (char) timerTicks);
                break;
            case CLKINT_TOGGLEINT:
                intMsg = cpu.getreg(Dcpu.Reg.B);
                break;
        }
    }
}
