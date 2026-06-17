package me.deecaad.core.mechanics.scope;

/**
 * Thrown when a cast exceeds its {@link CastBudget} (call depth or total
 * actions). Caught at the top of the executor and logged. Unchecked so it
 * unwinds any in-progress recursion.
 */
public class CastAbortException extends RuntimeException {

    public CastAbortException(String message) {
        super(message);
    }
}
