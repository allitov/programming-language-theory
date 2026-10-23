package io.allitov.plt.automaton;

/**
 * Full result of the minimization pipeline.
 *
 * @param original     the input automaton (possibly an NFA)
 * @param determinism  verdict of the determinism check for {@code original}
 * @param determinized the DFA obtained by subset construction; the same instance
 *                     as {@code original} when the input was already deterministic
 * @param minimized    the minimal DFA
 */
public record MinimizationResult(FiniteAutomaton original,
                                 DeterminismReport determinism,
                                 FiniteAutomaton determinized,
                                 FiniteAutomaton minimized) {
}
