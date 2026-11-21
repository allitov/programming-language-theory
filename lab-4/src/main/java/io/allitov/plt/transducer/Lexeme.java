package io.allitov.plt.transducer;

/**
 * A lexical unit of the input line: {@code symbol} - the transducer input symbol ({@code num},
 * {@code +}, {@code (}, {@code ⊥} and so on), {@code text} - the original fragment of the line
 * (for a number it is its digit string), {@code position} - the zero-based position of the fragment
 * in the input line (used to localize errors).
 */
public record Lexeme(String symbol, String text, int position) {
}
