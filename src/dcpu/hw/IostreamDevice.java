package dcpu.hw;

import dcpu.Dcpu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Device for communicating with pair of i/o streams. For debugging/testing/demo purposes
 * <p/>
 * Interrupt behaviour depends on A:
 * 0:   Send character B to stdout
 * 1:   Set B = character from stdout, -1 if no input
 * 2:   Send zero-terminated string pointed by B to stdout. Also stops if reached 0xffff->0x0000 overflow
 */
public class IostreamDevice extends Dcpu.Device {

    public static final int HARDWARE_ID = str2id("IOSD");
    public static final int MANUFACTURER_ID = str2id("AIMF");

    public static final int SIOINT_PUTC = 0;
    public static final int SIOINT_GETC = 1;
    public static final int SIOINT_PUTS = 2;

    protected InputStream input;
    protected OutputStream output;

    public IostreamDevice(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
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
    public int getManufacturerId() {
        return MANUFACTURER_ID;
    }

    @Override
    public void interrupt() {
        switch (cpu.getreg(Dcpu.Reg.A)) {
            case SIOINT_PUTC:
                try {
                    output.write(cpu.getreg(Dcpu.Reg.B) & 0xffff);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case SIOINT_GETC:
                try {
                    if (input.available() > 0) {
                        cpu.setreg(Dcpu.Reg.B, (short) input.read());
                    } else {
                        cpu.setreg(Dcpu.Reg.B, (short) -1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    cpu.setreg(Dcpu.Reg.B, (short) -1);
                }
                break;
            case SIOINT_PUTS:
                int addr = cpu.getreg(Dcpu.Reg.B) & 0xffff;
                while (true) {
                    short val = cpu.mem[addr];
                    if (val == 0) break;
                    try {
                        output.write(val & 0xffff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    addr++;
                    if (addr == Dcpu.RAM_SIZE) break;
                }
                break;
        }
    }
}
