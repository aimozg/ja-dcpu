package dcpu.io;

import dcpu.Dcpu;
import dcpu.Listener;

import java.io.IOException;
import java.io.InputStream;

/**
 * Returns characters from input stream (non-blocking) when read at any offset.
 */
public class InstreamPeripheral extends Dcpu.Peripheral {

    volatile boolean attached = false;
    private int bufsize;

    @Override
    public void attachedTo(Dcpu cpu, int baseaddr) {
        attached = true;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    while (attached) {
                        int c = input.read();
                        if (c != -1 && (wo + 1) % bufsize != ro) synchronized (buffer) {
                            buffer[wo] = (char) c;
                            wo = (wo + 1) % bufsize;
                        }
                    }
                } catch (IOException e) {
                    if (exceptionListener != null) {
                        exceptionListener.preExecute(e);
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        }, "StdinPeripheralReader");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void detached() {
        attached = false;
    }

    Listener<IOException> exceptionListener;

    public void setExceptionListener(Listener<IOException> exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    public InstreamPeripheral(InputStream input, int bufsize) {
        this.input = input;
        if (bufsize < 2) bufsize = 2;
        buffer = new char[bufsize];
        this.bufsize = bufsize;
        ro = 0;
        wo = 0;
    }

    int ro, wo;
    InputStream input;
    final char[] buffer;

    @Override
    public char onMemget(int offset) {
        if (ro != wo) synchronized (buffer) {
            char result = buffer[ro];
            ro = (ro + 1) % bufsize;
            return result;
        }
        else return 0xffff;
    }
}
