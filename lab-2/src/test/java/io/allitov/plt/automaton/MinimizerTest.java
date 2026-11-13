package io.allitov.plt.automaton;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static io.allitov.plt.automaton.TestAutomata.automaton;
import static io.allitov.plt.automaton.TestAutomata.endsWithA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MinimizerTest {

    private final Minimizer minimizer = new Minimizer();

    @Test
    void removesUnreachableStates() {
        FiniteAutomaton dfa = automaton(Set.of("q0", "q1", "q9"), Set.of("a"), "q0", Set.of("q1"),
            new Transition("q0", "a", "q1"),
            new Transition("q9", "a", "q0"));
        FiniteAutomaton minimized = minimizer.minimize(dfa);
        assertThat(minimized.getStates()).containsExactlyInAnyOrder("q0", "q1");
        Languages.assertEquivalent(dfa, minimized, 5);
    }

    @Test
    void mergesEquivalentStates() {
        FiniteAutomaton dfa = automaton(Set.of("q0", "q1", "q2", "q3"), Set.of("a", "b"), "q0", Set.of("q2", "q3"),
            new Transition("q0", "a", "q1"),
            new Transition("q0", "b", "q2"),
            new Transition("q1", "a", "q3"),
            new Transition("q1", "b", "q2"),
            new Transition("q2", "a", "q2"),
            new Transition("q2", "b", "q2"),
            new Transition("q3", "a", "q3"),
            new Transition("q3", "b", "q3"));
        FiniteAutomaton minimized = minimizer.minimize(dfa);
        assertThat(minimized.getStates()).containsExactlyInAnyOrder("q0", "q1", "{q2,q3}");
        assertThat(minimized.getFinals()).containsExactly("{q2,q3}");
        assertThat(minimized.getStart()).isEqualTo("q0");
        Languages.assertEquivalent(dfa, minimized, 6);
    }

    @Test
    void alreadyMinimalAutomatonIsUnchanged() {
        FiniteAutomaton dfa = endsWithA();
        assertThat(minimizer.minimize(dfa)).isEqualTo(dfa);
    }

    @Test
    void allFinalStatesCollapseIntoOne() {
        FiniteAutomaton dfa = automaton(Set.of("q0", "q1"), Set.of("a"), "q0", Set.of("q0", "q1"),
            new Transition("q0", "a", "q1"),
            new Transition("q1", "a", "q1"));
        FiniteAutomaton minimized = minimizer.minimize(dfa);
        assertThat(minimized.getStates()).containsExactly("{q0,q1}");
        assertThat(minimized.getStart()).isEqualTo("{q0,q1}");
        assertThat(minimized.getFinals()).containsExactly("{q0,q1}");
        assertThat(minimized.getTransitions()).containsExactly(new Transition("{q0,q1}", "a", "{q0,q1}"));
    }

    @Test
    void partialDfaStaysPartialWithoutTrapStates() {
        FiniteAutomaton dfa = automaton(Set.of("q0", "q1", "q2", "q3"), Set.of("a", "b"), "q0", Set.of("q3"),
            new Transition("q0", "a", "q1"),
            new Transition("q0", "b", "q2"),
            new Transition("q1", "a", "q3"),
            new Transition("q2", "a", "q3"));
        FiniteAutomaton minimized = minimizer.minimize(dfa);
        assertThat(minimized.getStates()).containsExactlyInAnyOrder("q0", "{q1,q2}", "q3");
        assertThat(minimized.getAlphabet()).containsExactlyInAnyOrder("a", "b");
        assertThat(minimized.getTransitions()).containsExactlyInAnyOrder(
            new Transition("q0", "a", "{q1,q2}"),
            new Transition("q0", "b", "{q1,q2}"),
            new Transition("{q1,q2}", "a", "q3"));
        Languages.assertEquivalent(dfa, minimized, 5);
    }

    @Test
    void rejectsNonDeterministicInput() {
        FiniteAutomaton nfa = automaton(Set.of("q0", "q1", "q2"), Set.of("a"), "q0", Set.of("q2"),
            new Transition("q0", "a", "q1"),
            new Transition("q0", "a", "q2"));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> minimizer.minimize(nfa))
            .withMessageContaining("deterministic");
    }
}
