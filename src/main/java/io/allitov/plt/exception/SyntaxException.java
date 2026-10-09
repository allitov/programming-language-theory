package io.allitov.plt.exception;

public class SyntaxException extends RuntimeException {

    private final int position;

    public SyntaxException(String message, int position) {
        super(message);
        this.position = position;
    }

    public int position() {
        return position;
    }
}
