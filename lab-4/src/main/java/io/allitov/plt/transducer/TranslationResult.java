package io.allitov.plt.transducer;

import java.util.List;

/**
 * A successful translation: the initial configuration, the final configuration and all ticks.
 * The resulting RPN is the output tape of the final configuration (a chain of lexemes, rendered
 * with separators when printed).
 */
public record TranslationResult(Configuration initial, Configuration last, List<Step> steps) {

    public TranslationResult {
        steps = List.copyOf(steps);
    }

    public List<String> output() {
        return last.output();
    }
}
