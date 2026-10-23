package io.allitov.plt.automaton;

import java.util.List;

/**
 * Result of the determinism check. A non-deterministic automaton is accompanied
 * by a list of human-readable violations.
 *
 * @param deterministic true when the automaton is a DFA
 * @param violations    reasons of non-determinism, empty for a DFA
 */
public record DeterminismReport(boolean deterministic, List<String> violations) {

    public DeterminismReport {
        violations = List.copyOf(violations);
    }
}
