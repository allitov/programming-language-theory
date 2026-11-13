package io.allitov.plt.automaton;

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
