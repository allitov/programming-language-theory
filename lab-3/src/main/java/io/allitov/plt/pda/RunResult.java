package io.allitov.plt.pda;

import java.util.List;

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
