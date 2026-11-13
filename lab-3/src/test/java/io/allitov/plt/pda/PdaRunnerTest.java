package io.allitov.plt.pda;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdaRunnerTest {

    private static final String ANBN = """
        # L = { a^n b^n | n >= 1 }
        states: q0 q1 q2 q3
        alphabet: a b
        stack: Z A
        start: q0
        final: q3

        transitions:
        q0 a Z = q1 AZ
        q1 a A = q1 AA
        q1 b A = q2 λ
        q2 b A = q2 λ
        q2 λ Z = q3 λ
        """;

    private final PdaRunner runner = new PdaRunner();

    @Test
    void acceptsStringsOfTheLanguage() {
        PushdownAutomaton automaton = PdaParser.parse(ANBN);
        for (String chain : List.of("ab", "aabb", "aaabbb")) {
            RunResult result = runner.run(automaton, chain);
            assertThat(result.accepted()).as("chain '%s'", chain).isTrue();
            assertThat(result.reason()).as("chain '%s'", chain).isEmpty();
        }
    }

    @Test
    void producesExpectedTraceForAb() {
        PushdownAutomaton automaton = PdaParser.parse(ANBN);
        RunResult result = runner.run(automaton, "ab");
        assertThat(result.steps()).hasSize(3);
        assertThat(result.steps()).satisfiesExactly(
            step -> assertThat(PdaPrinter.formatStep(step)).isEqualTo("1) (q0, ab, Z) -> (q1, b, AZ)"),
            step -> assertThat(PdaPrinter.formatStep(step)).isEqualTo("2) (q1, b, AZ) -> (q2, λ, Z)"),
            step -> assertThat(PdaPrinter.formatStep(step)).isEqualTo("3) (q2, λ, Z) -> (q3, λ, λ)"));
        assertThat(result.last().state()).isEqualTo("q3");
        assertThat(result.last().stack()).isEmpty();
    }

    @Test
    void rejectsStringWithUnbalancedAAsAsNonFinalState() {
        RunResult result = runner.run(PdaParser.parse(ANBN), "aab");
        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).isEqualTo("the input has been fully consumed but the automaton stopped in state 'q2', "
            + "which is not a final state (final states: [q3])");
    }

    @Test
    void rejectsEmptyStringWhenStartStateIsNotFinal() {
        RunResult result = runner.run(PdaParser.parse(ANBN), "λ");
        assertThat(result.accepted()).isFalse();
        assertThat(result.steps()).isEmpty();
        assertThat(result.reason()).isEqualTo("the input has been fully consumed but the automaton stopped in state 'q0', "
            + "which is not a final state (final states: [q3])");
    }

    @Test
    void rejectsStringStuckOnFirstSymbol() {
        RunResult result = runner.run(PdaParser.parse(ANBN), "b");
        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).isEqualTo("no transition is defined for the configuration (q0, b, Z): "
            + "the automaton got stuck while reading symbol 'b' (position 1)");
    }

    @Test
    void rejectsStringStuckAfterLambdaMove() {
        RunResult result = runner.run(PdaParser.parse(ANBN), "abab");
        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).isEqualTo("no transition is defined for the configuration (q3, ab, λ): "
            + "the automaton got stuck while reading symbol 'a' (position 3)");
    }

    @Test
    void rejectsSymbolOutsideOfAlphabet() {
        RunResult result = runner.run(PdaParser.parse(ANBN), "axb");
        assertThat(result.accepted()).isFalse();
        assertThat(result.steps()).isEmpty();
        assertThat(result.reason()).isEqualTo("the string contains the symbol 'x' (position 2), "
            + "which is not in the automaton alphabet [a, b]");
    }

    @Test
    void tokenizesByLongestMatch() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1 q2
            alphabet: ab a b
            stack: Z
            start: q0
            final: q2
            transitions:
            q0 ab Z = q1 Z
            q1 a Z = q2 Z
            """);
        RunResult result = runner.run(automaton, "aba");
        assertThat(result.accepted()).isTrue();
        assertThat(result.steps()).hasSize(2);
    }

    @Test
    void acceptsImmediatelyWhenStartStateIsFinal() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q0
            transitions:
            q0 a Z = q1 Z
            """);
        RunResult result = runner.run(automaton, "λ");
        assertThat(result.accepted()).isTrue();
        assertThat(result.steps()).isEmpty();
        assertThat(result.initial().state()).isEqualTo("q0");
    }

    @Test
    void lambdaMoveDoesNotConsumeInput() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 λ Z = q1 Z
            """);
        RunResult result = runner.run(automaton, "a");
        assertThat(result.steps()).hasSize(1);
        assertThat(result.last().input()).isNotEmpty();
        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).isEqualTo("no transition is defined for the configuration (q1, a, Z): "
            + "the automaton got stuck while reading symbol 'a' (position 1)");
    }

    @Test
    void stepLimitStopsLambdaLoop() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 λ Z = q0 Z
            """);
        RunResult result = runner.run(automaton, "λ");
        assertThat(result.accepted()).isFalse();
        assertThat(result.steps()).hasSize(PdaRunner.STEP_LIMIT);
        assertThat(result.reason()).contains("step limit").contains("λ-transitions");
    }

    @Test
    void acceptsByFinalStateReachedAfterLastInputSymbol() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 a Z = q1 Z
            """);
        assertThat(runner.run(automaton, "a").accepted()).isTrue();
        assertThat(runner.run(automaton, "aa").accepted()).isFalse();
    }
}
