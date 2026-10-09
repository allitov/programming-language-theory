package io.allitov.plt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void analyzesValidExpressionWithParseTree() {
        String result = Main.analyze("2 + 3 * 4");

        assertThat(result).startsWith("Выражение корректно.\nS\n");
        assertThat(result).contains("E' (ε)");
        assertThat(result).contains("number '3'");
    }

    @Test
    void analyzesValidParenthesizedExpression() {
        String result = Main.analyze("a * (b - 10)");

        assertThat(result).startsWith("Выражение корректно.\nS\n");
        assertThat(result).contains("id 'a'");
    }

    @Test
    void returnsExpectedTokenError() {
        assertThat(Main.analyze("5 + + 3"))
                .isEqualTo("Ошибка в позиции 4. Ожидалось: number, id или '('");
    }

    @Test
    void returnsMissingParenthesisError() {
        assertThat(Main.analyze("(7 * 2"))
                .isEqualTo("Ошибка в позиции 6. Ожидалось: ')'");
    }

    @Test
    void returnsLexicalError() {
        assertThat(Main.analyze("1 @ 2"))
                .isEqualTo("Ошибка в позиции 2: недопустимый символ '@'");
    }
}
