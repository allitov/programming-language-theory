package io.allitov.plt.pda;

import java.util.List;

/**
 * A single move of the pushdown automaton.
 *
 * <p>The move is applicable in a configuration whose state is {@code from}, whose next input symbol
 * is {@code input} (or whose input is empty, when {@code input} is λ) and whose stack top is {@code top}.
 * After the move the automaton switches to state {@code to} and replaces the stack top with {@code push}:
 * the list holds the new symbols in order from the new top towards the bottom, and an empty list
 * means the top symbol is removed from the stack.</p>
 */
public record PdaTransition(String from, String input, String top, String to, List<String> push) {

    public PdaTransition {
        push = List.copyOf(push);
    }
}
