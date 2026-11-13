package io.allitov.plt.automaton;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static io.allitov.plt.automaton.TestAutomata.automaton;
import static io.allitov.plt.automaton.TestAutomata.endsWith01Nfa;
import static io.allitov.plt.automaton.TestAutomata.endsWithA;
import static org.assertj.core.api.Assertions.assertThat;

class DeterminizerTest {

    private final Determinizer determinizer = new Determinizer();
    private final DeterminismChecker checker = new DeterminismChecker();

    @Test
    void deterministicAutomatonIsRebuiltIdentically() {
        FiniteAutomaton dfa = endsWithA();
        assertThat(determinizer.determinize(dfa)).isEqualTo(dfa);
    }

    @Test
    void subsetConstructionProducesEquivalentDfa() {
        FiniteAutomaton nfa = endsWith01Nfa();
        FiniteAutomaton dfa = determinizer.determinize(nfa);
        assertThat(checker.check(dfa).deterministic()).isTrue();
        assertThat(dfa.getStates()).containsExactlyInAnyOrder("q0", "{q0,q1}", "{q0,q2}");
        assertThat(dfa.getFinals()).containsExactly("{q0,q2}");
        assertThat(dfa.getStart()).isEqualTo("q0");
        assertThat(dfa.acceptsWord("01")).isTrue();
        assertThat(dfa.acceptsWord("1001")).isTrue();
        assertThat(dfa.acceptsWord("10")).isFalse();
        Languages.assertEquivalent(nfa, dfa, 6);
    }

    @Test
    void epsilonTransitionsAreAbsorbedIntoClosures() {
        FiniteAutomaton nfa = automaton(Set.of("q0", "q1", "q2"), Set.of("a"), "q0", Set.of("q2"),
            new Transition("q0", FiniteAutomaton.EPSILON, "q1"),
            new Transition("q1", "a", "q2"),
            new Transition("q2", FiniteAutomaton.EPSILON, "q1"));
        FiniteAutomaton dfa = determinizer.determinize(nfa);
        assertThat(checker.check(dfa).deterministic()).isTrue();
        assertThat(dfa.getStates()).containsExactlyInAnyOrder("{q0,q1}", "{q1,q2}");
        assertThat(dfa.acceptsWord("a")).isTrue();
        assertThat(dfa.acceptsWord("aa")).isTrue();
        assertThat(dfa.acceptsWord("")).isFalse();
        Languages.assertEquivalent(nfa, dfa, 5);
    }
}
