package io.allitov.plt.transducer;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransducerPrinter: configuration and tick formatting")
class TransducerPrinterTest {

    @Test
    void formatsConfigurationAsQuadrupleWithLambdaForEmptyChains() {
        Configuration configuration = new Configuration("q1",
            List.of(new Lexeme("+", "+", 2), new Lexeme("num", "3", 4)),
            List.of("+", "Z"), "5");
        assertThat(TransducerPrinter.format(configuration)).isEqualTo("(q1, + 3, +Z, 5)");
    }

    @Test
    void usesLambdaForEmptyInputStackAndTape() {
        Configuration configuration = new Configuration("q8", List.of(), List.of("Z"), "");
        assertThat(TransducerPrinter.format(configuration)).isEqualTo("(q8, λ, Z, λ)");
    }

    @Test
    void formatsStepWithNumberAndArrow() {
        Configuration before = new Configuration("q0", List.of(new Lexeme("num", "5", 0)), List.of("Z"), "");
        Configuration after = new Configuration("q1", List.of(), List.of("Z"), "5");
        Step step = new Step(1, before, after,
            new Transition("q0", "num", "Z", "q1", List.of("Z"), List.of("num")), true);
        assertThat(TransducerPrinter.formatStep(step))
            .isEqualTo("1) (q0, 5, Z, λ) -> (q1, λ, Z, 5)");
    }

    @Test
    void formatsTransitionRule() {
        Transition transition = new Transition("q0", "num", "Z", "q1", List.of("Z"), List.of("num"));
        assertThat(TransducerPrinter.formatTransition(transition))
            .isEqualTo("δ(q0, num, Z) = (q1, Z, num)");
    }

    @Test
    void summaryDescribesAllComponents() {
        Transducer transducer = TestSupport.rpnTransducer();
        String summary = TransducerPrinter.summary(transducer);
        assertThat(summary)
            .contains("P = (Q, V, Z, V', δ, q0, z0, F)")
            .contains("States (Q): q0 q1 q2 q3 q4 q5 q6 q7 q8")
            .contains("Input alphabet (V): 'num', '+', '-', '*', '/', '(', ')', '⊥'")
            .contains("Stack alphabet (Z): 'Z', '(0', '(1', '+', '-', '*', '/'")
            .contains("Output alphabet (V'): ")
            .contains("Start state (q0): q0")
            .contains("Start stack symbol (z0): Z")
            .contains("Final states (F): q8")
            .contains("Transitions (|δ|): 92")
            .contains("δ(q0, num, Z) = (q1, Z, num)");
    }
}
