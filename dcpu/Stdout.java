package dcpu;

/**
 * Simple peripheral that outputs all characters written to its line
 * to stdout
 */
public class Stdout extends NotchDcpu.Peripheral{
    @Override
    public void onMemset(int addr, short newval, short oldval) {
        System.out.print((char)newval);
    }
}
