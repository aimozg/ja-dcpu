package dcpu;

/**
 * Prints executed commands to stdout
 */
public class Tracer implements Listener<Short> {

    Dcpu dcpu;
    Disassembler disassembler;

    public void install(Dcpu dcpu) {
        this.dcpu = dcpu;
        dcpu.stepListener = this;
        disassembler = new Disassembler();
        disassembler.init(dcpu.mem);
    }

    public void event(Short pc) {
        disassembler.setAddress(pc & 0xffff);
        System.out.printf("%04x: %s\n", pc, disassembler.next());
    }
}
