package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.hw.IostreamDevice;

/**
 * Asks user's name and greets him.
 * <p/>
 * Updated to use IostreamDevice
 */
public class StdinoutDemo {
    public static void main(String[] args) {
        Dcpu cpu = new Dcpu();
        cpu.upload(new Assembler().assemble(DemoUtils.getDemoAsmReader(StdinoutDemo.class)));

        IostreamDevice stdio = new IostreamDevice(System.in, System.out);
        cpu.attach(stdio);

        cpu.reset();
        cpu.run();

    }
}
