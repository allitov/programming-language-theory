package io.allitov.plt.transducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.allitov.plt.exception.TranslationException;
import lombok.experimental.UtilityClass;

/**
 * Lexical analysis of an input line: digits are grouped into {@code num} lexemes, whitespace acts as
 * a lexeme separator and is skipped, the operation and parenthesis characters become single-symbol
 * lexemes. Finally the input chain end marker {@code ⊥} is appended, so the transducer can detect
 * the end of the input by a real transition instead of guessing it.
 */
@UtilityClass
public class Lexer {

    private static final String SINGLE_CHAR_SYMBOLS = "+-*/()";

    public List<Lexeme> tokenize(String line) {
        Objects.requireNonNull(line, "line");
        List<Lexeme> lexemes = new ArrayList<>();
        int position = 0;
        while (position < line.length()) {
            char current = line.charAt(position);
            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }
            if (current >= '0' && current <= '9') {
                int start = position;
                while (position < line.length() && line.charAt(position) >= '0' && line.charAt(position) <= '9') {
                    position++;
                }
                String text = line.substring(start, position);
                lexemes.add(new Lexeme("num", text, start));
                continue;
            }
            if (SINGLE_CHAR_SYMBOLS.indexOf(current) >= 0) {
                String text = String.valueOf(current);
                lexemes.add(new Lexeme(text, text, position));
                position++;
                continue;
            }
            throw new TranslationException("the expression contains the symbol '" + current + "' (position "
                + (position + 1) + "), which is not in the input alphabet", position);
        }
        if (lexemes.isEmpty()) {
            throw new TranslationException("the expression is empty", 0);
        }
        lexemes.add(new Lexeme(Transducer.INPUT_END, Transducer.INPUT_END, line.length()));
        return List.copyOf(lexemes);
    }
}
