package io.allitov.plt.pda;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdaPrinterTest {

    @Test
    void formatsConfiguration() {
        assertThat(PdaPrinter.format(new Configuration("q1", List.of("b", "c"), List.of("a", "Z"))))
            .isEqualTo("(q1, bc, aZ)");
        assertThat(PdaPrinter.format(new Configuration("q2", List.of(), List.of())))
            .isEqualTo("(q2, λ, λ)");
        assertThat(PdaPrinter.format(new Configuration("q0", List.of("a", "b"), List.of("Z"))))
            .isEqualTo("(q0, ab, Z)");
    }

    @Test
    void formatsStepWithNumber() {
        Step step = new Step(1,
            new Configuration("q0", List.of("a", "b"), List.of("Z")),
            new Configuration("q1", List.of("b"), List.of("A", "Z")),
            new PdaTransition("q0", "a", "Z", "q1", List.of("A", "Z")));
        assertThat(PdaPrinter.formatStep(step)).isEqualTo("1) (q0, ab, Z) -> (q1, b, AZ)");
    }

    @Test
    void formatsTransitionLikeTheInputFormat() {
        assertThat(PdaPrinter.formatTransition(new PdaTransition("q0", "a", "Z", "q1", List.of("A", "Z"))))
            .isEqualTo("q0 a Z = q1 AZ");
        assertThat(PdaPrinter.formatTransition(new PdaTransition("q1", "b", "A", "q2", List.of())))
            .isEqualTo("q1 b A = q2 λ");
        assertThat(PdaPrinter.formatTransition(new PdaTransition("q2", PushdownAutomaton.LAMBDA, "Z", "q3", List.of())))
            .isEqualTo("q2 λ Z = q3 λ");
    }

    @Test
    void formatsChainWithLambdaForEmptyString() {
        assertThat(PdaPrinter.formatChain("")).isEqualTo("λ");
        assertThat(PdaPrinter.formatChain("ab")).isEqualTo("ab");
        assertThat(PdaPrinter.formatChain("λ")).isEqualTo("λ");
    }

    @Test
    void printsSummary() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1 q2 q3
            alphabet: a b
            stack: Z A
            start: q0
            final: q3
            transitions:
            q0 a Z = q1 AZ
            q1 b A = q2 λ
            q2 λ Z = q3 λ
            """);
        assertThat(PdaPrinter.summary(automaton)).contains(
            "Automaton loaded:",
            "States: q0 q1 q2 q3",
            "Input alphabet: a b",
            "Stack alphabet: Z A",
            "Start state: q0",
            "Final states: q3",
            "Transitions:",
            "q0 a Z = q1 AZ",
            "q1 b A = q2 λ",
            "q2 λ Z = q3 λ");
    }

    @Test
    void printsPlaceholdersForEmptySets() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0
            alphabet: a
            stack: Z
            start: q0
            transitions:
            """);
        assertThat(PdaPrinter.summary(automaton)).contains("Final states: (none)").contains("Transitions: (none)");
    }
}
