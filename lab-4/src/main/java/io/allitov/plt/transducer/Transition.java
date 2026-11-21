package io.allitov.plt.transducer;

import java.util.List;

/**
 * A single rule of the transition function {@code δ(q, a, z) = (r, α, t)}: from state {@code state},
 * reading input symbol {@code input} (or {@code λ}) with {@code stackTop} on top of the stack, move to
 * {@code newState}, replace {@code stackTop} with the chain {@code push} (λ - pop, the same symbol - keep
 * the stack unchanged) and emit the chain {@code output} onto the output tape.
 *
 * <p>The {@code push} and {@code output} chains are stored as lists of atomic symbols; in an {@code output}
 * chain the symbol {@code num} denotes the text of the lexeme being read (echo of the input lexeme).
 */
public record Transition(String state, String input, String stackTop, String newState,
                         List<String> push, List<String> output) {

    /** The output-tape token that is replaced by the text of the consumed {@code num} lexeme. */
    public static final String NUM_TOKEN = "num";

    public Transition {
        push = List.copyOf(push);
        output = List.copyOf(output);
    }
}
