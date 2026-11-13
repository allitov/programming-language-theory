package io.allitov.plt.automaton;

import java.util.List;

public record DeterminismReport(boolean deterministic, List<String> violations) {

    public DeterminismReport {
        violations = List.copyOf(violations);
    }
}
