package io.allitov.plt.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.allitov.plt.exception.SyntaxException;
import io.allitov.plt.lexer.Lexer;
import io.allitov.plt.tree.TreePrinter;
import org.junit.jupiter.api.Test;

class PartialParseTreeTest {

    @Test
    void buildsPartialTreeForMissingPrimary() {
        assertThatThrownBy(() -> new Parser(new Lexer("5 + +").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    String tree = TreePrinter.render(exception.partialTree());

                    assertThat(tree).contains("S\n", "E'", "'+'", "number '5'", "G\n", "error");
                    assertThat(tree).contains("F\n", "'+'");
                    assertThat(tree).containsSubsequence("error", "T' (ε)", "E' (ε)");
                });
    }

    @Test
    void buildsPartialTreeForMissingOperandAfterUnarySign() {
        assertThatThrownBy(() -> new Parser(new Lexer("-").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    String tree = TreePrinter.render(exception.partialTree());

                    assertThat(tree).containsSubsequence("S\n", "E\n", "F\n", "'-'", "G\n", "error");
                });
    }

    @Test
    void buildsPartialTreeForMissingClosingParenthesis() {
        assertThatThrownBy(() -> new Parser(new Lexer("(7 * 2").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    String tree = TreePrinter.render(exception.partialTree());

                    assertThat(tree).contains("S\n", "F\n", "G\n", "'('", "number '7'", "'*'", "number '2'");
                    assertThat(tree).containsSubsequence("error", "T' (ε)", "E' (ε)");
                });
    }

    @Test
    void buildsFullSuccessfulSubtreeForTrailingInput() {
        assertThatThrownBy(() -> new Parser(new Lexer("2 3").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    assertThat(TreePrinter.render(exception.partialTree()))
                            .containsSubsequence("S\n", "E\n", "number '2'");
                });
    }

    @Test
    void rendersBranchesParsedAfterTheError() {
        assertThatThrownBy(() -> new Parser(new Lexer("2 + * 3 + 4").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    String tree = TreePrinter.render(exception.partialTree());

                    assertThat(exception).hasMessage("Error at position 4. Expected: number, id or '('");
                    assertThat(tree).containsSubsequence(
                            "error", "'*'", "number '3'", "+ '+'", "number '4'");
                    assertThat(tree).contains("S\n", "E'\n", "T'\n", "T' (ε)", "E' (ε)");
                });
    }
}
