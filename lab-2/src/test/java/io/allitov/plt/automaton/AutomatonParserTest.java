package io.allitov.plt.automaton;

import io.allitov.plt.exception.AutomatonFormatException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutomatonParserTest {

    @Test
    void parsesCompleteAutomaton() {
        String text = """
            # words ending with "ab"
            states: q0 q1 q2
            alphabet: a b
            start: q0
            final: q2

            transitions:
            q0 a q0 q1
            q0 b q0
            q1 a q1
            q1 b q2
            q1 eps q0
            """;
        FiniteAutomaton automaton = AutomatonParser.parse(text);
        assertThat(automaton.getStates()).containsExactlyInAnyOrder("q0", "q1", "q2");
        assertThat(automaton.getAlphabet()).containsExactlyInAnyOrder("a", "b");
        assertThat(automaton.getStart()).isEqualTo("q0");
        assertThat(automaton.getFinals()).containsExactly("q2");
        assertThat(automaton.getTransitions()).containsExactlyInAnyOrder(
            new Transition("q0", "a", "q0"),
            new Transition("q0", "a", "q1"),
            new Transition("q0", "b", "q0"),
            new Transition("q1", "a", "q1"),
            new Transition("q1", "b", "q2"),
            new Transition("q1", FiniteAutomaton.EPSILON, "q0"));
    }

    @Test
    void acceptsEpsilonAliases() {
        String text = """
            states: q0 q1
            alphabet: a
            start: q0
            final: q1
            transitions:
            q0 \u03B5 q1
            q0 eps q1
            """;
        FiniteAutomaton automaton = AutomatonParser.parse(text);
        assertThat(automaton.getTransitions())
            .containsExactly(new Transition("q0", FiniteAutomaton.EPSILON, "q1"));
    }

    @Test
    void toleratesMissingTransitionsSection() {
        FiniteAutomaton automaton = AutomatonParser.parse("""
            states: q0
            alphabet: a
            start: q0
            final: q0
            """);
        assertThat(automaton.getTransitions()).isEmpty();
        assertThat(automaton.acceptsWord("")).isTrue();
    }

    @Test
    void reportsLineNumberOfUnknownState() {
        String text = """
            states: q0
            alphabet: a
            start: q0
            final: q0
            transitions:
            q0 a q7
            """;
        assertThatThrownBy(() -> AutomatonParser.parse(text))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 6")
            .hasMessageContaining("q7");
    }

    @Test
    void reportsUnknownSymbol() {
        String text = """
            states: q0 q1
            alphabet: a
            start: q0
            final: q1
            transitions:
            q0 c q1
            """;
        assertThatThrownBy(() -> AutomatonParser.parse(text))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 6")
            .hasMessageContaining("'c'");
    }

    @Test
    void failsWhenTransitionHasNoTarget() {
        String text = """
            states: q0 q1
            alphabet: a
            start: q0
            final: q1
            transitions:
            q0 a
            """;
        assertThatThrownBy(() -> AutomatonParser.parse(text))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 6");
    }

    @Test
    void failsWithoutStart() {
        assertThatThrownBy(() -> AutomatonParser.parse("states: q0\nalphabet: a\n"))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("start");
    }

    @Test
    void failsWhenStartNotDeclared() {
        assertThatThrownBy(() -> AutomatonParser.parse("""
            states: q0
            alphabet: a
            start: q5
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("q5");
    }

    @Test
    void failsOnDuplicateSection() {
        assertThatThrownBy(() -> AutomatonParser.parse("""
            states: q0
            states: q1
            alphabet: a
            start: q0
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("duplicate");
    }

    @Test
    void failsOnUnknownKey() {
        assertThatThrownBy(() -> AutomatonParser.parse("state: q0\nstart: q0\n"))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("unknown key");
    }

    @Test
    void failsOnContentOutsideSections() {
        assertThatThrownBy(() -> AutomatonParser.parse("""
            states: q0 q1
            alphabet: a
            start: q0
            final: q1
            q0 a q1
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("outside of any section");
    }
}
