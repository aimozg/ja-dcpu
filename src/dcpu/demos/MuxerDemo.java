package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.io.InstreamPeripheral;
import dcpu.io.Muxer;
import dcpu.io.OutstreamPeripheral;

/**
 * Demo of muxer.
 * <p/>
 * Muxer is attached to 0x9---, and has 16 bands. Stdin is attached to 0x90--, Stdout to 0x91--
 */
public class MuxerDemo {
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();
        cpu.upload(new Assembler().assemble("" +
                "; Read character from stdin (0x9000) and write to stdout (0x9100)\n" +
                ":read\n" +
                "     SET A,[0x9000]\n" +
                "     IFE A,0xFFFF\n" +
                "         SET PC,read\n" +
                "     SET [0x9100],A"));

        Muxer muxer = new Muxer(4, 8);
        InstreamPeripheral input = new InstreamPeripheral(System.in, 1);
        OutstreamPeripheral output = new OutstreamPeripheral(System.out);
        cpu.attach(muxer, 0x9);
        muxer.setPeripheral(0, input);
        muxer.setPeripheral(1, output);

        cpu.run();
    }
}
