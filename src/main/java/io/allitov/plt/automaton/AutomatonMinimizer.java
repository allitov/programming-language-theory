package io.allitov.plt.automaton;

/**
 * Facade of the whole pipeline: takes an arbitrary finite automaton (NFA) and returns
 * the minimal DFA, checking determinism along the way.
 *
 * <ol>
 *     <li>{@link DeterminismChecker} decides whether the input is already deterministic;</li>
 *     <li>if it is not, {@link Determinizer} builds an equivalent DFA by subset construction;</li>
 *     <li>{@link Minimizer} removes unreachable states and merges equivalent ones.</li>
 * </ol>
 */
public final class AutomatonMinimizer {

    private final DeterminismChecker checker = new DeterminismChecker();
    private final Determinizer determinizer = new Determinizer();
    private final Minimizer minimizer = new Minimizer();

    public MinimizationResult minimize(FiniteAutomaton automaton) {
        DeterminismReport report = checker.check(automaton);
        FiniteAutomaton deterministic = report.deterministic() ? automaton : determinizer.determinize(automaton);
        FiniteAutomaton minimized = minimizer.minimize(deterministic);
        return new MinimizationResult(automaton, report, deterministic, minimized);
    }
}
