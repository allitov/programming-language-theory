package io.allitov.plt.transducer;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("Transducer model: lookup and validation")
class TransducerTest {

    private static Transition transition(String state, String input, String stackTop, String newState,
                                         List<String> push, List<String> output) {
        return new Transition(state, input, stackTop, newState, push, output);
    }

    private static Transducer transducer(List<Transition> transitions) {
        return new Transducer(Set.of("q0", "qf"), Set.of("a"), Set.of("Z"), Set.of("x"),
            "q0", "Z", Set.of("qf"), transitions);
    }

    @Test
    void returnsRuleByTransitionKey() {
        Transition rule = transition("q0", "a", "Z", "qf", List.of("Z"), List.of("x"));
        Transducer transducer = transducer(List.of(rule));
        assertThat(transducer.transition("q0", "a", "Z")).contains(rule);
        assertThat(transducer.transition("q0", "a", "x")).isEmpty();
        assertThat(transducer.transition("q0", "λ", "Z")).isEmpty();
    }

    @Test
    void duplicateRuleForSameTripleIsRejected() {
        Transition first = transition("q0", "a", "Z", "qf", List.of("Z"), List.of());
        Transition second = transition("q0", "a", "Z", "qf", List.of(), List.of("x"));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> transducer(List.of(first, second)))
            .withMessageContaining("not deterministic")
            .withMessageContaining("more than once");
    }

    @Test
    void lambdaRuleConflictingWithRealRuleForSamePairIsRejected() {
        Transition real = transition("q0", "a", "Z", "qf", List.of("Z"), List.of());
        Transition lambda = transition("q0", "λ", "Z", "qf", List.of("Z"), List.of());
        assertThatIllegalArgumentException()
            .isThrownBy(() -> transducer(List.of(real, lambda)))
            .withMessageContaining("conflicts with a transition by a real input symbol");
    }

    @Test
    void lambdaRuleAloneForPairIsAccepted() {
        Transition lambda = transition("qf", "λ", "Z", "qf", List.of("Z"), List.of());
        assertThat(transducer(List.of(lambda)).getTransitions()).hasSize(1);
    }

    @Test
    void lambdaMustNotBeDeclaredAsRealSymbol() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Transducer(Set.of("q0"), Set.of("λ"), Set.of("Z"), Set.of("x"),
                "q0", "Z", Set.of("q0"), List.of()))
            .withMessageContaining("denotes emptiness");
    }

    @Test
    void startStackSymbolMustBelongToStackAlphabet() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Transducer(Set.of("q0"), Set.of("a"), Set.of("Z"), Set.of("x"),
                "q0", "A", Set.of("q0"), List.of()))
            .withMessageContaining("start stack symbol");
    }

    @Test
    void outputTokensMustBelongToOutputAlphabet() {
        Transition wrong = transition("q0", "a", "Z", "q0", List.of("Z"), List.of("y"));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> transducer(List.of(wrong)))
            .withMessageContaining("output token 'y'");
    }
}
