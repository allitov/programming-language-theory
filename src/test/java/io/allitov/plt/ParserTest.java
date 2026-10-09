package io.allitov.plt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParserTest {

    @Test
    void parsesExpressionsFollowingOperatorPrecedence() {
        List<Node> roots = List.of("2 + 3 * 4", "a * (b - 10)", "1 / (2 + 3) - x").stream()
                .map(input -> new Parser(new Lexer(input).tokenize()).parse())
                .toList();

        assertThat(roots).allSatisfy(root -> assertThat(root.label()).isEqualTo("S"));
        assertThat(roots.getFirst().children().getFirst().label()).isEqualTo("E");
    }

    @Test
    void parsesNestedParentheses() {
        Node root = new Parser(new Lexer("((42))").tokenize()).parse();

        Node firstF = root.children().getFirst().children().getFirst().children().getFirst();
        assertThat(firstF.label()).isEqualTo("F");
        assertThat(firstF.children()).hasSize(3);
        assertThat(firstF.children().get(1).label()).isEqualTo("S");
    }

    @Test
    void reportsMissingPrimaryWithPosition() {
        assertThatThrownBy(() -> new Parser(new Lexer("5 + + 3").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Ошибка в позиции 4. Ожидалось: number, id или '('");
                    assertThat(exception.position()).isEqualTo(4);
                });
    }

    @Test
    void reportsMissingClosingParenthesisWithPosition() {
        assertThatThrownBy(() -> new Parser(new Lexer("(7 * 2").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Ошибка в позиции 6. Ожидалось: ')'");
                    assertThat(exception.position()).isEqualTo(6);
                });
    }

    @Test
    void reportsTrailingInput() {
        assertThatThrownBy(() -> new Parser(new Lexer("2 3").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Ошибка в позиции 2. Ожидалось: конец ввода");
                    assertThat(exception.position()).isEqualTo(2);
                });
    }

    @Test
    void reportsEmptyInput() {
        assertThatThrownBy(() -> new Parser(new Lexer("  ").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Ошибка в позиции 2. Ожидалось: number, id или '('");
                    assertThat(exception.position()).isEqualTo(2);
                });
    }

    @Test
    void rejectsInvalidParserArguments() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Parser(null))
                .withMessage("Список токенов не должен быть null");
        assertThatThrownBy(new Parser(List.of())::parse)
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Ошибка в позиции 0. Ожидалось: number, id или '('");
                    assertThat(exception.position()).isEqualTo(0);
                });
    }
}
