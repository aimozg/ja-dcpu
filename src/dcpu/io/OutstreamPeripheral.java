package dcpu.io;

import dcpu.Dcpu;
import dcpu.Listener;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Simple peripheral that outputs all characters written to its line
 * to output stream. Flushes after each write
 */
public class OutstreamPeripheral extends Dcpu.Peripheral {

    final OutputStream output;
    Listener<IOException> exceptionListener;

    public void setExceptionListener(Listener<IOException> exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    public OutstreamPeripheral(OutputStream output) {
        this.output = output;
    }

    @Override
    public void onMemset(int offset, char newval, char oldval) {
        try {
            output.write(newval);
            output.flush();
        } catch (IOException e) {
            if (exceptionListener != null) {
                exceptionListener.preExecute(e);
            } else {
                e.printStackTrace();
            }
        }
    }
}
