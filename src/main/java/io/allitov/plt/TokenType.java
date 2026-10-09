package io.allitov.plt;

public enum TokenType {
    NUMBER("number"),
    ID("id"),
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    EOF("end of input");

    private final String displayName;

    TokenType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
