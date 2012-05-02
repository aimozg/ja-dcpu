package dcpu.hw;

import dcpu.Dcpu;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MonitorLEM1802 extends Dcpu.Device implements IMonitor {

    public static final int HARDWARE_ID = 0x7349f615;
    public static final char HARDWARE_VERSION = 0x1802;
    public static final int MANUFACTURER_ID = 0x1c6c8b36;

    /**
     * Reads the B register, and maps the video ram to DCPU-16 ram starting
     * at address B. See below for a description of video ram.
     * If B is 0, the screen is disconnected.
     * When the screen goes from 0 to any other value, the the LEM1802 takes
     * about one second to start up. Other interrupts sent during this time
     * are still processed.
     */
    public static final char MONINT_MEM_MAP_SCREEN = 0;
    /**
     * Reads the B register, and maps the font ram to DCPU-16 ram starting
     * at address B. See below for a description of font ram.
     * If B is 0, the default font is used instead.
     */
    public static final char MONINT_MEM_MAP_FONT = 1;
    /**
     * Reads the B register, and maps the palette ram to DCPU-16 ram starting
     * at address B. See below for a description of palette ram.
     * If B is 0, the default palette is used instead.
     */
    public static final char MONINT_MEM_MAP_PALETTE = 2;
    /**
     * Reads the B register, and sets the border color to palette index B&0xF
     */
    public static final char MONINT_SET_BORDER_COLOR = 3;
    /**
     * Reads the B register, and writes the default font data to DCPU-16 ram
     * starting at address B.
     * Halts the DCPU-16 for 256 cycles
     */
    public static final char MONINT_MEM_DUMP_FONT = 4;
    /**
     * Reads the B register, and writes the default palette data to DCPU-16
     * ram starting at address B.
     * Halts the DCPU-16 for 16 cycles
     */
    public static final char MONINT_MEM_DUMP_PALETTE = 5;

    public static final int MON_ROWS = 12;
    public static final int MON_COLS = 32;

    public static final int CHAR_W = 4;
    public static final int CHAR_H = 8;

    public static final int CELL_FG_MASK = 0xf000;
    public static final int CELL_FG_SHIFT = 12;
    public static final int CELL_BG_MASK = 0x0f00;
    public static final int CELL_BG_SHIFT = 8;
    public static final int CELL_BLINK_MASK = 0x0080;
    public static final int CELL_BLINK_SHIFT = 7;
    public static final int CELL_CHAR_MASK = 0x007f;

    public static final int PAL_R_MASK = 0x0f00;
    public static final int PAL_G_MASK = 0x00f0;
    public static final int PAL_B_MASK = 0x000f;
    public static final int PAL_R_SHIFT = 8;
    public static final int PAL_G_SHIFT = 4;

    @Override
    public int getHeight() {
        return MON_ROWS * CHAR_H;
    }

    @Override
    public int getWidth() {
        return MON_COLS * CHAR_W;
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

    private static final char[] DEFAULT_FONT = new char[256];
    private static final char[] DEFAULT_PALETTE = {
            0x0000,//--- (black)
            0x0007,//--b (dark blue)
            0x0070,//-g- (dark green)
            0x0077,//-gb (dark teal)
            0x0700,//r-- (dark red)
            0x0707,//r-b (dark purple)
            0x0770,//rg- (dark yellow)
            0x0555,//rgb (dark gray)
            0x0aaa,//rgb (light gray)
            0x000F,//--B (blue)
            0x00F0,//-G- (green)
            0x00FF,//-GB (teal)
            0x0F00,//R-- (red)
            0x0F0F,//R-B (purple)
            0x0FF0,//RG- (yellow)
            0x0FFF //RGB (white)
    };

    static {
        int pixels[] = new int[0x1000];
        BufferedImage image;
        // dump font
        try {
            image = ImageIO.read(MonitorLEM1802.class.getResource("lem1802font.png"));
            image.getRGB(0, 0, 128, 32, pixels, 0, 128);
        } catch (IOException e) {
            e.printStackTrace();
            image = null;
        }
        final int ROWS = 4;
        final int COLS = 128 / ROWS;
        for (int i = 0; i < 128; i++) {
            int word0 = 0;
            int word1 = 0;

            /* "F":

            word0 = 11111111 x=0 (0)
                    00001001 x=1 (1)
            word1 = 00001001 x=0 (2)
                    00000000 x=1 (3)

                  y=76543210
             */
            int x0 = (i % COLS) * 4;
            int y0 = (i / COLS) * 8;
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 8; y++) {
                    if ((pixels[(y0 + y) * 128 + (x0 + x + 0)] & 0x00ffffff) != 0) {
                        word0 |= (1 << y) << ((1 - x) * 8);
                    }
                    if ((pixels[(y0 + y) * 128 + (x0 + x + 2)] & 0x00ffffff) != 0) {
                        word1 |= (1 << y) << ((1 - x) * 8);
                    }
                }
            }

            DEFAULT_FONT[i * 2 + 0] = (char) word0;
            DEFAULT_FONT[i * 2 + 1] = (char) word1;
        }
    }

    private int BLINK_FREQUENCY = 4;

    private boolean active = false;
    private char vram_offset = 0;
    private char[] fontbuffer = DEFAULT_FONT;
    private char fontoffset = 0;
    private char[] palbuffer = DEFAULT_PALETTE;
    private char paloffset = 0;
    private char borderColor = 0;

    @Override
    public int getBorderColorRGB() {
        return mkcolor(borderColor);
    }

    @Override
    public void render(int[] pixels) {
        if (!active) return;

        long time = System.currentTimeMillis() * BLINK_FREQUENCY * 2 / 1000;
        boolean blink = time % 2L == 0L;

        // Decoding 0x0rgb --> 0x00rrggbb
        int pal[] = new int[16];
        for (int i = 0; i < 16; i++) {
            pal[i] = mkcolor(i);
        }

        for (int row = 0; row < MON_ROWS; row++) {
            for (int col = 0; col < MON_COLS; col++) {
                char cell = cpu.mem[(vram_offset + row * MON_COLS + col) & 0xffff];
                int fg = (cell & CELL_FG_MASK) >> CELL_FG_SHIFT;
                int bg = (cell & CELL_BG_MASK) >> CELL_BG_SHIFT;
                boolean blinkbit = (cell & CELL_BLINK_MASK) != 0;
                int chara = cell & CELL_CHAR_MASK;

                int fgcol = (blink && blinkbit) ? pal[bg] : pal[fg];
                int bgcol = pal[bg];

                int fontbase = (fontoffset + 2 * chara) % fontbuffer.length;
                int pixbase = row * 4 * 8 * MON_COLS + col * 4;
                for (int x = 0; x < 4; x++) {
                    int bits = fontbuffer[fontbase + (x >> 1)];
                    if ((x & 1) == 0) bits >>= 8;
                    for (int y = 0; y < 8; y++) {
                        pixels[pixbase + x + y * 4 * MON_COLS] = (bits & (1 << y)) != 0 ? fgcol : bgcol;
                    }
                }
            }
        }
    }

    private int mkcolor(int idx) {
        char color = palbuffer[(paloffset + idx) % palbuffer.length];
        int r = (color & PAL_R_MASK) >> PAL_R_SHIFT;
        int g = (color & PAL_G_MASK) >> PAL_G_SHIFT;
        int b = color & PAL_B_MASK;
        r = r | (r <<= 4);
        g = g | (g << 4);
        b = b | (b << 4);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void interrupt() {
        char b = cpu.getreg(Dcpu.Reg.B);
        switch (cpu.getreg(Dcpu.Reg.A)) {
            case MONINT_MEM_MAP_SCREEN:
                vram_offset = b;
                active = (b != 0);
                // TODO setup time:
//                When the screen goes from 0 to any other value, the the LEM1802 takes
//                       about one second to start up. Other interrupts sent during this time
//                       are still processed.
                break;
            case MONINT_MEM_MAP_FONT:
                if (b == 0) {
                    fontbuffer = DEFAULT_FONT;
                    fontoffset = 0;
                } else {
                    fontbuffer = cpu.mem;
                    fontoffset = b;
                }
                break;
            case MONINT_MEM_MAP_PALETTE:
                if (b == 0) {
                    palbuffer = DEFAULT_PALETTE;
                    paloffset = 0;
                } else {
                    palbuffer = cpu.mem;
                    paloffset = b;
                }
                break;
            case MONINT_SET_BORDER_COLOR:
                borderColor = (char) (b & 0xf);
                break;
            case MONINT_MEM_DUMP_FONT:
                for (int i = 0; i < DEFAULT_FONT.length; i++) {
                    cpu.mem[(b + i) & 0xffff] = DEFAULT_FONT[i];
                }
                cpu.tickWait(256);
                break;
            case MONINT_MEM_DUMP_PALETTE:
                for (int i = 0; i < DEFAULT_PALETTE.length; i++) {
                    cpu.mem[(b + i) & 0xffff] = DEFAULT_PALETTE[i];
                }
                cpu.tickWait(16);
                break;
        }
    }
}
