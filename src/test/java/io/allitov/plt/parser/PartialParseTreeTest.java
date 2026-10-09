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
        assertThatThrownBy(() -> new Parser(new Lexer("5 + + 3").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    String tree = TreePrinter.render(exception.partialTree());

                    assertThat(tree).contains("S\n", "E'", "'+'", "number '5'", "error");
                    assertThat(tree).doesNotContain("number '3'");
                });
    }

    @Test
    void buildsPartialTreeForMissingClosingParenthesis() {
        assertThatThrownBy(() -> new Parser(new Lexer("(7 * 2").tokenize()).parse())
                .isInstanceOfSatisfying(SyntaxException.class, exception -> {
                    String tree = TreePrinter.render(exception.partialTree());

                    assertThat(tree).contains("S\n", "F\n", "'('", "number '7'", "'*'", "number '2'");
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
}
