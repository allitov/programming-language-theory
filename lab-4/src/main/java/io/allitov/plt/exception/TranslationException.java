package io.allitov.plt.exception;

import lombok.Getter;

/**
 * Signals that an input expression cannot be translated: invalid symbol, unexpected lexeme,
 * unbalanced parentheses, a dangling operation and so on.
 *
 * <p>Carries the zero-based position (of a character for lexical errors or of a lexeme for
 * syntax errors) at which the problem was detected.
 */
@Getter
public class TranslationException extends RuntimeException {

    private final int position;

    public TranslationException(String message, int position) {
        super(message);
        this.position = position;
    }
}
