package io.allitov.plt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerTest {

    @Test
    void splitsAllSupportedTokens() {
        List<Token> tokens = new Lexer("count + 2.5 - value * 3 / (x - 10)").tokenize();

        assertThat(tokens).containsExactly(
                new Token(TokenType.ID, "count", 0),
                new Token(TokenType.PLUS, "+", 6),
                new Token(TokenType.NUMBER, "2.5", 8),
                new Token(TokenType.MINUS, "-", 12),
                new Token(TokenType.ID, "value", 14),
                new Token(TokenType.MULTIPLY, "*", 20),
                new Token(TokenType.NUMBER, "3", 22),
                new Token(TokenType.DIVIDE, "/", 24),
                new Token(TokenType.LEFT_PAREN, "(", 26),
                new Token(TokenType.ID, "x", 27),
                new Token(TokenType.MINUS, "-", 29),
                new Token(TokenType.NUMBER, "10", 31),
                new Token(TokenType.RIGHT_PAREN, ")", 33),
                new Token(TokenType.EOF, "", 34));
    }

    @Test
    void skipsWhitespaceAndReturnsEndOfInput() {
        List<Token> tokens = new Lexer("  \t\r\n42 ").tokenize();

        assertThat(tokens).containsExactly(
                new Token(TokenType.NUMBER, "42", 5),
                new Token(TokenType.EOF, "", 8));
    }

    @Test
    void reportsInvalidCharacterPosition() {
        assertThatThrownBy(() -> new Lexer("1 @ 2").tokenize())
                .isInstanceOfSatisfying(LexicalException.class, exception -> {
                    assertThat(exception).hasMessage("Ошибка в позиции 2: недопустимый символ '@'");
                    assertThat(exception.position()).isEqualTo(2);
                });
    }

    @Test
    void rejectsNullInput() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Lexer(null))
                .withMessage("Входная строка не должна быть null");
    }
}
