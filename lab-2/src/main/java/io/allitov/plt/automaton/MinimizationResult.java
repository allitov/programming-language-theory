package io.allitov.plt.automaton;

public record MinimizationResult(FiniteAutomaton original,
                                 DeterminismReport determinism,
                                 FiniteAutomaton determinized,
                                 FiniteAutomaton minimized) {
}
