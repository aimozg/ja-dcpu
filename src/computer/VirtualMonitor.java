package computer;

import javax.imageio.ImageIO;
import java.io.IOException;

/*
 * Taken from Notch's disassembled application - char changed to short
 */
public class VirtualMonitor {

    public VirtualMonitor(char[] ram, int offset) {
        this.pixels = new int[0x4000];
        this.ram = ram;
        this.offset = offset;
        charOffset = offset + 0x180;
        miscDataOffset = charOffset + 0x100;
        for (int i = 0; i < 0x100; i++) {
            int bg = genColor(i % 16);
            int fg = genColor(i / 16);
            colorBase[i] = bg;
            colorOffs[i] = fg - bg;
        }

        int pixels[] = new int[0x1000];
        try {
            ImageIO.read(getClass().getResource("font.png")).getRGB(0, 0, 128, 32, pixels, 0, 128);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int c = 0; c < 128; c++) {
            int ro = charOffset + c * 2;
            int xo = (c % 32) * 4;
            int yo = (c / 32) * 8;
            ram[ro + 0] = '\0';
            ram[ro + 1] = '\0';
            for (int xx = 0; xx < 4; xx++) {
                int bb = 0;
                for (int yy = 0; yy < 8; yy++)
                    if ((pixels[xo + xx + (yo + yy) * 128] & 0xff) > 128)
                        bb |= 1 << yy;

                ram[ro + xx / 2] |= bb << (xx + 1 & 1) * 8;
            }

        }
    }

    private static int genColor(int i) {
        int b = (i >> 0 & 1) * 170;
        int g = (i >> 1 & 1) * 170;
        int r = (i >> 2 & 1) * 170;
        if (i == 6)
            g -= 85;
        else if (i >= 8) {
            r += 85;
            g += 85;
            b += 85;
        }
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    public void render() {
        long time = System.currentTimeMillis() / 16L;
        boolean blink = (time / 20L) % 2L == 0L;
        long reds = 0L;
        long greens = 0L;
        long blues = 0L;
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 32; x++) {
                char dat = (char) ram[offset + x + y * 32];
                int ch = dat & 0x7f;
                int colorIndex = dat >> 8 & 0xff;
                int co = charOffset + ch * 2;
                int color = colorBase[colorIndex];
                int colorAdd = colorOffs[colorIndex];
                if (blink && (dat & 0x80) > 0)
                    colorAdd = 0;
                int pixelOffs = x * 4 + y * 8 * 128;
                for (int xx = 0; xx < 4; xx++) {
                    int bits = ram[co + (xx >> 1)] >> (xx + 1 & 1) * 8 & 0xff;
                    for (int yy = 0; yy < 8; yy++) {
                        int col = color + colorAdd * (bits >> yy & 1);
                        pixels[pixelOffs + xx + yy * 128] = col;
                        reds += col & 0xff0000;
                        greens += col & 0xff00;
                        blues += col & 0xff;
                    }

                }

            }
        }

        int color = colorBase[ram[miscDataOffset] & 0xf];
        for (int y = 96; y < 128; y++) {
            for (int x = 0; x < 128; x++)
                pixels[x + y * 128] = color;

        }

        int borderPixels = 100;
        reds += (color & 0xff0000) * borderPixels;
        greens += (color & 0x00ff00) * borderPixels;
        blues += (color & 0x0000ff) * borderPixels;
        reds = reds / (long) (0x003000 + borderPixels) & 0xff0000L;
        greens = greens / (long) (0x003000 + borderPixels) & 0x00ff00L;
        blues = blues / (long) (0x003000 + borderPixels) & 0x0000ffL;
        lightColor = (int) (reds | greens | blues);
    }

    public int getBackgroundColor() {
        return colorBase[ram[miscDataOffset] & 0xf];
    }

    public void setPixels(int pixels[]) {
        this.pixels = pixels;
    }

    public int getLightColor() {
        return lightColor;
    }

    public static final int WIDTH_CHARS = 32;
    public static final int HEIGHT_CHARS = 12;
    public static final int WIDTH_PIXELS = 128;
    public static final int HEIGHT_PIXELS = 96;
    private final char[] ram;
    private final int offset;
    private final int charOffset;
    private final int miscDataOffset;
    private final int colorBase[] = new int[256];
    private final int colorOffs[] = new int[256];
    private int lightColor;
    public int pixels[];
}