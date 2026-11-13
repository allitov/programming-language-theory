package io.allitov.plt.token;

public record Token(TokenType type, String value, int position) {

    public Token {
        if (type == null) {
            throw new IllegalArgumentException("Token type must not be null");
        }
        if (value == null || (value.isEmpty() && type != TokenType.EOF)) {
            throw new IllegalArgumentException("Token value must not be empty");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Token position must be non-negative");
        }
    }
}
