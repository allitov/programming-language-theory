package io.allitov.plt.transducer;

import java.util.List;

public record Transition(String state, String input, String stackTop, String newState,
                         List<String> push, List<String> output) {

    public static final String NUM_TOKEN = "num";

    public Transition {
        push = List.copyOf(push);
        output = List.copyOf(output);
    }
}
