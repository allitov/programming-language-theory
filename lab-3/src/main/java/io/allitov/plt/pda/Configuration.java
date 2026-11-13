package io.allitov.plt.pda;

import java.util.List;

/**
 * An instantaneous description of the automaton: the current state, the not yet consumed part of the
 * input and the stack contents. Both the input and the stack are stored with the "next" symbol first:
 * the first input element is the symbol about to be read, the first stack element is the top of the stack.
 */
public record Configuration(String state, List<String> input, List<String> stack) {

    public Configuration {
        input = List.copyOf(input);
        stack = List.copyOf(stack);
    }
}
