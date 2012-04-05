package dcpu.io;

import dcpu.Dcpu;
import dcpu.Listener;

import java.io.IOException;
import java.io.InputStream;

/**
 * Returns characters from standard input (non-blocking) when read at any offset.
 */
public class InstreamPeripheral extends Dcpu.Peripheral {

    volatile boolean attached = false;

    @Override
    public void attachedTo(Dcpu cpu, int baseaddr) {
        attached = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (attached) {
                        int c = input.read();
                        if (c != -1 && ro != wo) synchronized (buffer) {
                            buffer[wo] = (short) c;
                            wo = (wo + 1) % buffer.length;
                        }
                    }
                } catch (IOException e) {
                    if (exceptionListener != null) {
                        exceptionListener.event(e);
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        }, "StdinPeripheralReader").start();
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
        buffer = new short[bufsize];
    }

    int ro, wo;
    InputStream input;
    final short[] buffer;

    @Override
    public short onMemget(int offset) {
        if (ro != wo) synchronized (buffer) {
            short result = buffer[ro];
            ro = (ro + 1) % buffer.length;
            return result;
        }
        else return (short) 0xffff;
    }
}
