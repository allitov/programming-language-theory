package io.allitov.plt.automaton;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether a finite automaton is deterministic.
 *
 * <p>An automaton is deterministic when it has no epsilon transitions and every
 * pair (state, symbol) is mapped to at most one target state. Missing transitions
 * are allowed: a partial DFA is still deterministic.</p>
 */
public final class DeterminismChecker {

    public DeterminismReport check(FiniteAutomaton automaton) {
        List<String> violations = new ArrayList<>();
        List<String> states = automaton.getStates().stream().sorted().toList();
        List<String> alphabet = automaton.getAlphabet().stream().sorted().toList();
        for (String state : states) {
            List<String> epsilonTargets = automaton.targets(state, FiniteAutomaton.EPSILON).stream().sorted().toList();
            if (!epsilonTargets.isEmpty()) {
                violations.add("state '" + state + "' has an epsilon transition to " + epsilonTargets);
            }
            for (String symbol : alphabet) {
                int targetCount = automaton.targets(state, symbol).size();
                if (targetCount > 1) {
                    violations.add("state '" + state + "' has " + targetCount + " transitions on symbol '" + symbol + "'");
                }
            }
        }
        return new DeterminismReport(violations.isEmpty(), violations);
    }
}
