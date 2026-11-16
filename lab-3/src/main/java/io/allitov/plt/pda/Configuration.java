package io.allitov.plt.pda;

import java.util.List;

public record Configuration(String state, List<String> input, List<String> stack) {

    public Configuration {
        input = List.copyOf(input);
        stack = List.copyOf(stack);
    }
}
