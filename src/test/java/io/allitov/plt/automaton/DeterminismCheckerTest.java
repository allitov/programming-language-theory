package io.allitov.plt.automaton;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static io.allitov.plt.automaton.TestAutomata.automaton;
import static org.assertj.core.api.Assertions.assertThat;

class DeterminismCheckerTest {

    private final DeterminismChecker checker = new DeterminismChecker();

    @Test
    void reportsDeterministicForCompleteDfa() {
        FiniteAutomaton dfa = TestAutomata.endsWithA();
        assertThat(checker.check(dfa).deterministic()).isTrue();
    }

    @Test
    void missingTransitionsKeepAutomatonDeterministic() {
        FiniteAutomaton partialDfa = automaton(Set.of("q0", "q1"), Set.of("a", "b"), "q0", Set.of("q1"),
            new Transition("q0", "a", "q1"));
        assertThat(checker.check(partialDfa).deterministic()).isTrue();
    }

    @Test
    void multipleTransitionsOnSameSymbolViolateDeterminism() {
        FiniteAutomaton nfa = automaton(Set.of("q0", "q1", "q2"), Set.of("a"), "q0", Set.of("q2"),
            new Transition("q0", "a", "q1"),
            new Transition("q0", "a", "q2"));
        DeterminismReport report = checker.check(nfa);
        assertThat(report.deterministic()).isFalse();
        assertThat(report.violations()).anySatisfy(violation -> {
            assertThat(violation).contains("q0");
            assertThat(violation).contains("'a'");
        });
    }

    @Test
    void epsilonTransitionsViolateDeterminism() {
        FiniteAutomaton nfa = automaton(Set.of("q0", "q1"), Set.of("a"), "q0", Set.of("q1"),
            new Transition("q0", FiniteAutomaton.EPSILON, "q1"));
        DeterminismReport report = checker.check(nfa);
        assertThat(report.deterministic()).isFalse();
        assertThat(report.violations()).anySatisfy(violation -> assertThat(violation).contains("epsilon"));
    }
}
