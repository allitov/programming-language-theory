package io.allitov.plt.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import io.allitov.plt.exception.SyntaxException;
import io.allitov.plt.lexer.Lexer;
import io.allitov.plt.tree.Node;
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

        Node firstG = root.children().getFirst().children().getFirst().children().getFirst().children().getFirst();
        assertThat(firstG.label()).isEqualTo("G");
        assertThat(firstG.children()).hasSize(3);
        assertThat(firstG.children().get(1).label()).isEqualTo("S");
    }

    @Test
    void parsesUnarySignedFactors() {
        List<Node> roots = List.of("-2", "+2", "-a", "+ (b - 10)", "2 * -3").stream()
                .map(input -> new Parser(new Lexer(input).tokenize()).parse())
                .toList();

        assertThat(roots).allSatisfy(root -> assertThat(root.label()).isEqualTo("S"));

        Node firstFactor = roots.getFirst().children().getFirst().children().getFirst().children().getFirst();
        assertThat(firstFactor.label()).isEqualTo("F");
        assertThat(firstFactor.children().getFirst().label()).isEqualTo("- '-'");
        Node firstOperand = firstFactor.children().get(1);
        assertThat(firstOperand.label()).isEqualTo("G");
        assertThat(firstOperand.children().getFirst().label()).isEqualTo("number '2'");
    }

    @Test
    void rejectsRepeatedUnarySigns() {
        List<String> expressions = List.of("--2", "++2", "-+2", "+-2");

        assertThat(expressions).allSatisfy(expression -> assertThatThrownBy(
                        () -> new Parser(new Lexer(expression).tokenize()).parse())
                .isInstanceOf(SyntaxException.class));
    }

    @Test
    void reportsMissingOperandAfterBinaryOperatorWithPosition() {
        assertThatThrownBy(() -> new Parser(new Lexer("5 + +").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Error at position 5. Expected: number, id or '('");
                    assertThat(exception.position()).isEqualTo(5);
                });
    }

    @Test
    void reportsMissingClosingParenthesisWithPosition() {
        assertThatThrownBy(() -> new Parser(new Lexer("(7 * 2").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Error at position 6. Expected: ')'");
                    assertThat(exception.position()).isEqualTo(6);
                });
    }

    @Test
    void reportsTrailingInput() {
        assertThatThrownBy(() -> new Parser(new Lexer("2 3").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Error at position 2. Expected: end of input");
                    assertThat(exception.position()).isEqualTo(2);
                });
    }

    @Test
    void reportsEmptyInput() {
        assertThatThrownBy(() -> new Parser(new Lexer("  ").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Error at position 2. Expected: number, id or '('");
                    assertThat(exception.position()).isEqualTo(2);
                });
    }

    @Test
    void rejectsInvalidParserArguments() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Parser(null))
                .withMessage("Token list must not be null");
        assertThatThrownBy(new Parser(List.of())::parse)
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(exception).hasMessage("Error at position 0. Expected: number, id or '('");
                    assertThat(exception.position()).isEqualTo(0);
                });
    }
}
