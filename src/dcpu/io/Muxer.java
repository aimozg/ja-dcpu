package dcpu.io;

import dcpu.Dcpu;

/**
 * Multiplexer to 2**x_bits lower-level peripherals, each guarding 2**m_bits words.
 * <p/>
 * Example. If you attach Muxer(muxBits=2,offBits=10) to line 0xa000.
 * Request for address
 * 1010 xxmm mmmm mmmm
 * will be handled by muxer's peripheral "xx" with offset "mmmmmmmmmm"
 * <p/>
 * You can further attach Muxer(muxBits=6,offButs=4) to band 0 of this Muxer.
 * Requests for address
 * 1010 00xx xxxx mmmm
 * will be handled by submuxer's peripheral "xxxxxx" with offset "mmmm"
 * <p/>
 * This enables hierarchical muxing, but watch for bits!
 * <p/>
 * If no peripheral is attached, muxer does plain memory reads/writes
 */
public class Muxer extends Dcpu.Peripheral {


    final int x_bits;
    final int m_bits;
    final int m_mask;
    Dcpu.Peripheral peripherals[];

    public void setPeripheral(int band, Dcpu.Peripheral peripheral) {
        if (peripherals[band] != null) {
            cpu.detach(peripherals[band]);
        }
        peripherals[band] = peripheral;
        if (peripheral != null) {
            cpu.attach(peripheral, -1);
            peripheral.baseaddr = baseaddr + (band << m_bits);
        }
    }

    public Muxer(int x_bits, int m_bits) {
        this.x_bits = x_bits;
        this.m_bits = m_bits;
        this.m_mask = (1 << m_bits) - 1;
        this.peripherals = new Dcpu.Peripheral[1 << x_bits];
    }

    @Override
    public void onMemset(int offset, short newval, short oldval) {
        int line = offset >> m_bits;
        if (peripherals[line] != null) {
            peripherals[line].onMemset(offset & m_mask, newval, oldval);
        } else {
            cpu.mem[baseaddr + offset] = newval;
        }
    }

    @Override
    public short onMemget(int offset) {
        int line = offset >> m_bits;
        if (peripherals[line] != null) {
            return peripherals[line].onMemget(offset & m_mask);
        } else {
            return cpu.mem[baseaddr + offset];
        }
    }

    @Override
    public void attachedTo(Dcpu cpu, int baseaddr) {
        super.attachedTo(cpu, baseaddr);
        for (Dcpu.Peripheral peripheral : peripherals) {
            if (peripheral != null) {
                cpu.attach(peripheral, -1);
            }
        }
    }

    @Override
    public void detached() {
        for (Dcpu.Peripheral peripheral : peripherals) {
            if (peripheral != null) {
                cpu.detach(peripheral);
            }
        }
        super.detached();
    }
}
