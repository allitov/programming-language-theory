package io.allitov.plt.pda;

import java.util.List;

/**
 * The outcome of one run: the initial and the last configuration, every executed step, the verdict
 * and, for a rejection, the reason why the string does not belong to the language.
 */
public record RunResult(Configuration initial, Configuration last, List<Step> steps, boolean accepted, String reason) {

    public RunResult {
        steps = List.copyOf(steps);
    }

    public static RunResult accepted(Configuration initial, Configuration last, List<Step> steps) {
        return new RunResult(initial, last, steps, true, "");
    }

    public static RunResult rejected(Configuration initial, Configuration last, List<Step> steps, String reason) {
        return new RunResult(initial, last, steps, false, reason);
    }
}
