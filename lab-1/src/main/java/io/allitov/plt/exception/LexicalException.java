package io.allitov.plt.exception;

public class LexicalException extends RuntimeException {

    private final int position;

    public LexicalException(String message, int position) {
        super(message);
        this.position = position;
    }

    public int position() {
        return position;
    }
}
