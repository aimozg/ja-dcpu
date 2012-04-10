package dcpu;

import java.util.LinkedList;
import java.util.List;

public class ListenerList<T> implements Listener<T> {

    private final List<Listener<T>> listeners = new LinkedList<Listener<T>>();

    public void event(T arg) {
        for (Listener<T> listener : listeners) {
            listener.event(arg);
        }
    }

    public void addListener(Listener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener<T> listener) {
        listeners.remove(listener);
    }
}
