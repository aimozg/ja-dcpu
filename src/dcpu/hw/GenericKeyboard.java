package dcpu.hw;

import dcpu.Dcpu;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayDeque;
import java.util.Queue;

import static java.awt.event.KeyEvent.*;

public class GenericKeyboard extends Dcpu.Device implements KeyListener {

    public static final int HARDWARE_ID = 0x30cf7406;
    public static final int HARDWARE_VERSION = 1;
    private final int manufacturerId;

    public static final int KBDINT_CLEAR_BUFFER = 0;
    public static final int KBDINT_GETKEY = 1;
    public static final int KBDINT_ISPRESSED = 2;
    public static final int KBDINT_TOGGLEINT = 3;

    public static final int KN_BACKSPACE = 0x10;
    public static final int KN_RETURN = 0x11;
    public static final int KN_INSERT = 0x12;
    public static final int KN_DELETE = 0x13;
    public static final int KN_UP = 0x80;
    public static final int KN_DOWN = 0x81;
    public static final int KN_LEFT = 0x82;
    public static final int KN_RIGHT = 0x83;
    public static final int KN_SHIFT = 0x90;
    public static final int KN_CONTROL = 0x91;

    // List of pairs VK_xxx, KN_xxx
    private static final int[] VK_TO_KN_MAP = {
            VK_BACK_SPACE, KN_BACKSPACE,
            VK_ENTER, KN_RETURN,
            VK_INSERT, KN_INSERT,
            VK_DELETE, KN_DELETE,
            VK_UP, KN_UP,
            VK_DOWN, KN_DOWN,
            VK_LEFT, KN_LEFT,
            VK_RIGHT, KN_RIGHT,
            VK_SHIFT, KN_SHIFT,
            VK_CONTROL, KN_CONTROL,
            //Numpad
            VK_KP_UP, KN_UP,
            VK_KP_DOWN, KN_DOWN,
            VK_KP_LEFT, KN_LEFT,
            VK_KP_RIGHT, KN_RIGHT
    };

    private Queue<Character> buffer;
    private char intMsg = 0;
    private boolean[] pressed = new boolean[256];
    private final int bufferSize;

    public GenericKeyboard(int manufacturerId, int bufferSize) {
        this.manufacturerId = manufacturerId;
        this.bufferSize = bufferSize;
        buffer = new ArrayDeque<Character>(bufferSize);
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
        return manufacturerId;
    }

    @Override
    public void interrupt() {
        char b = cpu.getreg(Dcpu.Reg.B);
        switch (cpu.getreg(Dcpu.Reg.A)) {
            case KBDINT_CLEAR_BUFFER:
                buffer.clear();
                break;
            case KBDINT_GETKEY:
                if (buffer.isEmpty()) {
                    cpu.setreg(Dcpu.Reg.C, (char) 0);
                } else {
                    cpu.setreg(Dcpu.Reg.C, buffer.remove());
                }
                break;
            case KBDINT_ISPRESSED:
                if (b > 0xff) {
                    cpu.setreg(Dcpu.Reg.C, (char) 0);
                } else {
                    cpu.setreg(Dcpu.Reg.C, (char) (pressed[b] ? 1 : 0));
                }
                break;
            case KBDINT_TOGGLEINT:
                intMsg = b;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() != CHAR_UNDEFINED) {
            if (buffer.size() < bufferSize) {
                buffer.add(e.getKeyChar());
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = getCode(e.getKeyCode());
        if (code >= 0) pressed[code] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = getCode(e.getKeyCode());
        if (code >= 0) pressed[code] = false;
    }

    private int getCode(int keyCode) {
        if (keyCode >= 0x20 && keyCode <= 0x7f) return keyCode;
        for (int i = 0; i < VK_TO_KN_MAP.length; i += 2) {
            if (keyCode == VK_TO_KN_MAP[i]) return VK_TO_KN_MAP[i + 1];
        }
        return -1;
    }
}
