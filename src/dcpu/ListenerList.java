package dcpu;

import java.util.LinkedList;
import java.util.List;

public class ListenerList<T> implements Listener<T> {

    private final List<Listener<T>> listeners = new LinkedList<Listener<T>>();

    @Override
    public void preExecute(T arg) {
        for (Listener<T> listener : listeners) {
            listener.preExecute(arg);
        }
    }

    @Override
    public void postExecute(T arg) {
        for (Listener<T> listener : listeners) {
            listener.postExecute(arg);
        }
    }

    public void addListener(Listener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener<T> listener) {
        listeners.remove(listener);
    }

}
