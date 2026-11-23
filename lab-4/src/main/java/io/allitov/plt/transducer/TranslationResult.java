package io.allitov.plt.transducer;

import java.util.List;

public record TranslationResult(Configuration initial, Configuration last, List<Step> steps) {

    public TranslationResult {
        steps = List.copyOf(steps);
    }

    public String output() {
        return last.output();
    }
}
