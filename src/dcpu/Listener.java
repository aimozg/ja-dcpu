package dcpu;

import java.util.EventListener;

public interface Listener<T> extends EventListener {
    public void preExecute(T pc);
    public void postExecute(T pc);
}
