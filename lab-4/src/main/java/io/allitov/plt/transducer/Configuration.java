package io.allitov.plt.transducer;

import java.util.List;

/**
 * A transducer configuration - the quadruple {@code (q, x, α, y)}: {@code state} - the current state,
 * {@code input} - the not yet processed part of the input chain, {@code stack} - the stack content
 * (the first element is the top), {@code output} - the output chain emitted so far.
 */
public record Configuration(String state, List<Lexeme> input, List<String> stack, String output) {

    public Configuration {
        input = List.copyOf(input);
        stack = List.copyOf(stack);
    }
}
