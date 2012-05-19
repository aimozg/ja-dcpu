package dcpu.antlr.node;

@SuppressWarnings("serial")
public class UnknownValueException extends RuntimeException {
    public UnknownValueException(String message) {
        super(message);
    }
}
