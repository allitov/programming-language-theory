package io.allitov.plt.transducer;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.allitov.plt.exception.TranslationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Lexer: line -> lexeme chain + end marker")
class LexerTest {

    @Test
    void tokenizesExpressionSkippingWhitespace() {
        List<Lexeme> lexemes = Lexer.tokenize("5 + 3* 2");
        assertThat(lexemes).extracting(Lexeme::symbol)
            .containsExactly("num", "+", "num", "*", "num", "⊥");
        assertThat(lexemes).extracting(Lexeme::text)
            .containsExactly("5", "+", "3", "*", "2", "⊥");
        assertThat(lexemes).extracting(Lexeme::position)
            .containsExactly(0, 2, 4, 5, 7, 8);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "12+345, num;+;num;⊥",
        "(7), (;num;);⊥",
        "  10 * (3 + 2) , num;*;(;num;+;num;);⊥"
    })
    void tokenizesWithoutSpaces(String line, String expected) {
        assertThat(Lexer.tokenize(line)).extracting(Lexeme::symbol)
            .containsExactly(expected.split(";"));
    }

    @Test
    void multiDigitNumberBecomesSingleLexemeWithOriginalText() {
        List<Lexeme> lexemes = Lexer.tokenize("12+345");
        assertThat(lexemes.get(0)).isEqualTo(new Lexeme("num", "12", 0));
        assertThat(lexemes.get(2)).isEqualTo(new Lexeme("num", "345", 3));
    }

    @Test
    void rejectsInvalidSymbolWithPosition() {
        assertThatThrownBy(() -> Lexer.tokenize("5 @ 2"))
            .isInstanceOf(TranslationException.class)
            .hasMessageContaining("'@'")
            .hasMessageContaining("position 3");
    }

    @ParameterizedTest(name = "'{0}' is rejected as empty")
    @CsvSource({"''", "'   '"})
    void rejectsEmptyExpression(String line) {
        assertThatThrownBy(() -> Lexer.tokenize(line))
            .isInstanceOf(TranslationException.class)
            .hasMessageContaining("empty");
    }
}
