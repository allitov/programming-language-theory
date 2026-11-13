package io.allitov.plt.pda;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushdownAutomatonTest {

    @Test
    void requiresInitialStackSymbol() {
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("S"), "q0", Set.of("q1"),
            new PdaTransition("q0", "a", "S", "q1", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("the stack alphabet must contain the initial stack symbol 'Z'");
    }

    @Test
    void requiresAtLeastOneState() {
        assertThatThrownBy(() -> new PushdownAutomaton(Set.of(), Set.of("a"), Set.of("Z"), "q0", Set.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one state");
    }

    @Test
    void rejectsStartStateOutsideOfStates() {
        assertThatThrownBy(() -> automaton(Set.of("q0"), Set.of("a"), Set.of("Z"), "q5", Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("start state 'q5' is not declared in states");
    }

    @Test
    void rejectsFinalStateOutsideOfStates() {
        assertThatThrownBy(() -> automaton(Set.of("q0"), Set.of("a"), Set.of("Z"), "q0", Set.of("q1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("final state 'q1' is not declared in states");
    }

    @Test
    void rejectsUnknownTransitionSourceAndTarget() {
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q7", "a", "Z", "q1", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("transition source 'q7' is not declared in states");
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q0", "a", "Z", "q9", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("transition target 'q9' is not declared in states");
    }

    @Test
    void rejectsUnknownTransitionInputAndStackSymbols() {
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q0", "c", "Z", "q1", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("transition input 'c' is not declared in the alphabet");
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q0", "a", "X", "q1", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("transition stack top 'X' is not declared in the stack alphabet");
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q0", "a", "Z", "q1", List.of("X"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("transition push symbol 'X' is not declared in the stack alphabet");
    }

    @Test
    void rejectsLambdaAsDeclaredSymbol() {
        assertThatThrownBy(() -> automaton(Set.of("q0"), Set.of("a", "λ"), Set.of("Z"), "q0", Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("denotes emptiness");
        assertThatThrownBy(() -> automaton(Set.of("q0"), Set.of("a"), Set.of("Z", "ε"), "q0", Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("denotes emptiness");
    }

    @Test
    void rejectsBlankSymbol() {
        assertThatThrownBy(() -> automaton(Set.of("q0"), Set.of(" "), Set.of("Z"), "q0", Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("alphabet symbols must not be blank");
    }

    @Test
    void rejectsDuplicateTransitionKey() {
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q0", "a", "Z", "q1", List.of()),
            new PdaTransition("q0", "a", "Z", "q1", List.of("Z"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not deterministic")
            .hasMessageContaining("(q0, a, Z) is defined more than once");
    }

    @Test
    void rejectsLambdaTransitionCombinedWithSymbolTransition() {
        assertThatThrownBy(() -> automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q0", PushdownAutomaton.LAMBDA, "Z", "q1", List.of()),
            new PdaTransition("q0", "a", "Z", "q1", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not deterministic")
            .hasMessageContaining("state 'q0' has both a λ-transition and a transition on input 'a' for stack top 'Z'");
    }

    @Test
    void allowsSameTopWithDifferentInputSymbols() {
        assertThatCode(() -> automaton(Set.of("q0", "q1"), Set.of("a", "b"), Set.of("Z"), "q0", Set.of(),
            new PdaTransition("q0", "a", "Z", "q1", List.of()),
            new PdaTransition("q0", "b", "Z", "q1", List.of())))
            .doesNotThrowAnyException();
    }

    @Test
    void looksUpTransitionByStateInputAndTop() {
        PdaTransition transition = new PdaTransition("q0", "a", "Z", "q1", List.of("Z"));
        PushdownAutomaton automaton = automaton(Set.of("q0", "q1"), Set.of("a"), Set.of("Z"), "q0", Set.of(), transition);
        assertThat(automaton.transition("q0", "a", "Z")).contains(transition);
        assertThat(automaton.transition("q0", "b", "Z")).isEmpty();
        assertThat(automaton.transition("q0", "a", "A")).isEmpty();
        assertThat(automaton.transition("q1", "a", "Z")).isEmpty();
    }

    @SafeVarargs
    private static PushdownAutomaton automaton(Set<String> states, Set<String> alphabet, Set<String> stack,
                                               String start, Set<String> finals, PdaTransition... transitions) {
        return new PushdownAutomaton(states, alphabet, stack, start, finals, List.of(transitions));
    }
}
