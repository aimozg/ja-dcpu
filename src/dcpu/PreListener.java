package dcpu;

public abstract class PreListener<T> implements Listener<T> {
    @Override public void postExecute(T pc) {}; // ignores post events
}
