package io.allitov.plt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("""
            \\s+
            |(?<number>\\d+(?:\\.\\d+)?)
            |(?<id>[A-Za-z_][A-Za-z0-9_]*)
            |(?<plus>\\+)
            |(?<minus>-)
            |(?<multiply>\\*)
            |(?<divide>/)
            |(?<leftParen>\\()
            |(?<rightParen>\\))
            """, Pattern.COMMENTS);

    private final String input;

    public Lexer(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string must not be null");
        }
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        while (position < input.length()) {
            Matcher matcher = TOKEN_PATTERN.matcher(input);
            matcher.region(position, input.length());
            if (!matcher.lookingAt()) {
                throw new LexicalException(
                        "Error at position %d: invalid character '%s'".formatted(position, input.charAt(position)),
                        position);
            }

            String text = input.substring(matcher.start(), matcher.end());
            if (!text.isBlank()) {
                tokens.add(new Token(tokenType(matcher), text, matcher.start()));
            }
            position = matcher.end();
        }

        tokens.add(new Token(TokenType.EOF, "", input.length()));
        return List.copyOf(tokens);
    }

    private TokenType tokenType(Matcher matcher) {
        if (matcher.group("number") != null) {
            return TokenType.NUMBER;
        }
        if (matcher.group("id") != null) {
            return TokenType.ID;
        }
        if (matcher.group("plus") != null) {
            return TokenType.PLUS;
        }
        if (matcher.group("minus") != null) {
            return TokenType.MINUS;
        }
        if (matcher.group("multiply") != null) {
            return TokenType.MULTIPLY;
        }
        if (matcher.group("divide") != null) {
            return TokenType.DIVIDE;
        }
        if (matcher.group("leftParen") != null) {
            return TokenType.LEFT_PAREN;
        }
        if (matcher.group("rightParen") != null) {
            return TokenType.RIGHT_PAREN;
        }
        throw new LexicalException("Failed to determine token type", matcher.start());
    }
}
