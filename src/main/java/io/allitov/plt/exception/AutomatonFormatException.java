package io.allitov.plt.exception;

/**
 * Signals that an automaton description cannot be parsed or violates the format rules.
 */
public class AutomatonFormatException extends RuntimeException {

    public AutomatonFormatException(String message) {
        super(message);
    }
}
