package me.deecaad.core.mechanics.expression;

/**
 * Thrown at parse time for a malformed expression (a syntax error, unknown
 * function/property, or bad arity). Callers convert this into a
 * {@code SerializerException} with a config location.
 */
public class ExpressionException extends RuntimeException {

    private final int index;

    public ExpressionException(int index, String message) {
        super(message);
        this.index = index;
    }

    /**
     * The character index in the source expression where the error occurred.
     */
    public int getIndex() {
        return index;
    }
}
