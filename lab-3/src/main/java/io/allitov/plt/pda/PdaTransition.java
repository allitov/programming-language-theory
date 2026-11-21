package io.allitov.plt.pda;

import java.util.List;

public record PdaTransition(String from, String input, String top, String to, List<String> push) {

    public PdaTransition {
        push = List.copyOf(push);
    }
}
