package io.allitov.plt.exception;

import lombok.Getter;

@Getter
public class TranslationException extends RuntimeException {

    private final int position;

    public TranslationException(String message, int position) {
        super(message);
        this.position = position;
    }
}
