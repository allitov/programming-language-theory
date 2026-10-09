package io.allitov.plt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void analyzesValidExpressionWithParseTree() {
        String result = Main.analyze("2 + 3 * 4");

        assertThat(result).startsWith("Expression is valid.\nS\n");
        assertThat(result).contains("E' (ε)");
        assertThat(result).contains("number '3'");
    }

    @Test
    void analyzesValidParenthesizedExpression() {
        String result = Main.analyze("a * (b - 10)");

        assertThat(result).startsWith("Expression is valid.\nS\n");
        assertThat(result).contains("id 'a'");
    }

    @Test
    void returnsExpectedTokenError() {
        String result = Main.analyze("5 + + 3");

        assertThat(result).contains("S\n", "number '5'", "error");
        assertThat(result).endsWith("Error at position 4. Expected: number, id or '('");
    }

    @Test
    void returnsMissingParenthesisError() {
        String result = Main.analyze("(7 * 2");

        assertThat(result).contains("S\n", "F\n", "number '7'");
        assertThat(result).endsWith("Error at position 6. Expected: ')'");
    }

    @Test
    void returnsLexicalError() {
        assertThat(Main.analyze("1 @ 2"))
                .isEqualTo("Error at position 2: invalid character '@'");
    }
}
