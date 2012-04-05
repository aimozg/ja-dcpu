package dcpu;

import java.util.EventListener;

public interface Listener<T> extends EventListener {
    public void event(T arg);
}
