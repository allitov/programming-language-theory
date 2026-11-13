package io.allitov.plt.automaton;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static io.allitov.plt.automaton.TestAutomata.automaton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FiniteAutomatonTest {

    @Test
    void acceptsEmptyWordOnlyWhenStartIsFinal() {
        FiniteAutomaton accepting = automaton(Set.of("q0"), Set.of("a"), "q0", Set.of("q0"));
        FiniteAutomaton rejecting = automaton(Set.of("q0"), Set.of("a"), "q0", Set.of());
        assertThat(accepting.acceptsWord("")).isTrue();
        assertThat(rejecting.acceptsWord("")).isFalse();
    }

    @Test
    void acceptsWhenAnyNfaBranchAccepts() {
        FiniteAutomaton nfa = automaton(Set.of("q0", "q1", "q2", "q3"), Set.of("a"), "q0", Set.of("q3"),
            new Transition("q0", "a", "q1"),
            new Transition("q0", "a", "q2"),
            new Transition("q1", "a", "q3"));
        assertThat(nfa.acceptsWord("aa")).isTrue();
        assertThat(nfa.acceptsWord("a")).isFalse();
        assertThat(nfa.acceptsWord("aaa")).isFalse();
    }

    @Test
    void epsilonClosureFollowsChainsSilently() {
        FiniteAutomaton nfa = automaton(Set.of("q0", "q1", "q2", "q3"), Set.of("a"), "q0", Set.of("q3"),
            new Transition("q0", FiniteAutomaton.EPSILON, "q1"),
            new Transition("q1", FiniteAutomaton.EPSILON, "q2"),
            new Transition("q2", FiniteAutomaton.EPSILON, "q3"));
        assertThat(nfa.epsilonClosure(Set.of("q0"))).containsExactlyInAnyOrder("q0", "q1", "q2", "q3");
        assertThat(nfa.acceptsWord("")).isTrue();
        assertThat(nfa.hasEpsilonTransitions()).isTrue();
    }

    @Test
    void rejectsStartOutsideStates() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> automaton(Set.of("q0"), Set.of("a"), "qx", Set.of()))
            .withMessageContaining("start");
    }

    @Test
    void rejectsFinalOutsideStates() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> automaton(Set.of("q0"), Set.of("a"), "q0", Set.of("q1")))
            .withMessageContaining("q1");
    }

    @Test
    void rejectsTransitionWithUnknownState() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), "q0", Set.of(),
                new Transition("q0", "a", "q9")))
            .withMessageContaining("q9");
    }

    @Test
    void rejectsTransitionWithUnknownSymbol() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), "q0", Set.of(),
                new Transition("q0", "b", "q1")))
            .withMessageContaining("'b'");
    }

    @Test
    void rejectsEpsilonListedInAlphabet() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> automaton(Set.of("q0"), Set.of(FiniteAutomaton.EPSILON), "q0", Set.of()))
            .withMessageContaining("epsilon");
    }

    @Test
    void rejectsAutomatonWithoutStates() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> automaton(Set.of(), Set.of("a"), "q0", Set.of()))
            .withMessageContaining("at least one state");
    }
}
