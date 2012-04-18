package dcpu;

public abstract class PostListener<T> implements Listener<T> {
    @Override public void preExecute(T pc) {}; // ignores pre events
}
