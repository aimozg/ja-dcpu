package computer;

/*
 * Taken from Notch's disassembled application
 */
public class VirtualKeyboard {

    public VirtualKeyboard(char ram[], int offset, KeyMapping keyMapping) {
        pp = 0;
        this.ram = ram;
        this.offset = offset;
        this.keyMapping = keyMapping;
    }

    public void keyTyped(int i) {
        if (i <= 0 || i > 127)
            return;
        if (ram[offset + pp] != 0) {
            return;
        } else {
            ram[offset + pp] = (char) i;
            pp = (pp + 1) & 0xf;
            return;
        }
    }

    public void keyPressed(int key) {
        int i = keyMapping.getKey(key);
        // bug here? was "80" decimal
        if (i < 0x80 || i > 255)
            return;
        if (ram[offset + pp] != 0) {
            return;
        } else {
            ram[offset + pp] = (char) i;
            pp = (pp + 1) & 0xf;
            return;
        }
    }

    public void keyReleased(int key) {
        int i = keyMapping.getKey(key);
        // bug here? was "80" decimal
        if (i < 0x80 || i > 0xff)
            return;
        if (ram[offset + pp] != 0) {
            return;
        } else {
            ram[offset + pp] = (char) (i | 0x100);
            pp = (pp + 1) & 0xf;
            return;
        }
    }

    public static final int KEY_UP = 128;
    public static final int KEY_DOWN = 129;
    public static final int KEY_LEFT = 130;
    public static final int KEY_RIGHT = 131;
    private final char ram[];
    private final int offset;
    private int pp;
    private KeyMapping keyMapping;
}