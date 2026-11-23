package io.allitov.plt.transducer;

import java.util.List;

public record Configuration(String state, List<Lexeme> input, List<String> stack, String output) {

    public Configuration {
        input = List.copyOf(input);
        stack = List.copyOf(stack);
    }
}
