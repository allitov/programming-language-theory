package io.allitov.plt.automaton;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static io.allitov.plt.automaton.TestAutomata.endsWith01Nfa;
import static io.allitov.plt.automaton.TestAutomata.endsWithA;
import static org.assertj.core.api.Assertions.assertThat;

class AutomatonMinimizerTest {

    private final AutomatonMinimizer minimizer = new AutomatonMinimizer();
    private final DeterminismChecker checker = new DeterminismChecker();

    @Test
    void minimizesNfaIntoMinimalDfa() {
        FiniteAutomaton nfa = endsWith01Nfa();
        MinimizationResult result = minimizer.minimize(nfa);
        assertThat(result.determinism().deterministic()).isFalse();
        assertThat(result.determinized().getStates()).hasSize(3);
        assertThat(result.minimized().getStates()).hasSize(3);
        assertThat(checker.check(result.minimized()).deterministic()).isTrue();
        Languages.assertEquivalent(nfa, result.minimized(), 6);
    }

    @Test
    void deterministicInputSkipsSubsetConstruction() {
        FiniteAutomaton dfa = endsWithA();
        MinimizationResult result = minimizer.minimize(dfa);
        assertThat(result.determinism().deterministic()).isTrue();
        assertThat(result.determinized()).isSameAs(dfa);
        assertThat(result.minimized()).isEqualTo(dfa);
    }

    @Test
    void epsilonNfaLanguageIsPreserved() {
        FiniteAutomaton nfa = new FiniteAutomaton(
            Set.of("q0", "q1", "q2", "q3"),
            Set.of("a", "b"),
            "q0",
            Set.of("q3"),
            List.of(
                new Transition("q0", FiniteAutomaton.EPSILON, "q1"),
                new Transition("q1", "a", "q1"),
                new Transition("q1", "b", "q2"),
                new Transition("q2", "a", "q1"),
                new Transition("q2", "b", "q3"),
                new Transition("q3", FiniteAutomaton.EPSILON, "q1")));
        MinimizationResult result = minimizer.minimize(nfa);
        assertThat(result.determinism().deterministic()).isFalse();
        assertThat(checker.check(result.minimized()).deterministic()).isTrue();
        Languages.assertEquivalent(nfa, result.minimized(), 5);
    }
}
