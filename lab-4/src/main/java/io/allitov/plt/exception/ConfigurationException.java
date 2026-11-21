package io.allitov.plt.exception;

/**
 * Signals that the json configuration of a pushdown transducer is missing, unreadable or invalid:
 * malformed json, missing required fields, symbols outside the declared alphabets or a violated
 * determinism requirement.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
