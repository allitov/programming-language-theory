package io.allitov.plt;

public record Token(TokenType type, String value, int position) {

    public Token {
        if (type == null) {
            throw new IllegalArgumentException("Тип токена не должен быть null");
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Значение токена не должно быть пустым");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Позиция токена должна быть неотрицательной");
        }
    }
}
