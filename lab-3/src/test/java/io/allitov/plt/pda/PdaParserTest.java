package io.allitov.plt.pda;

import java.util.List;

import io.allitov.plt.exception.AutomatonFormatException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdaParserTest {

    @Test
    void parsesCompleteAutomaton() {
        String text = """
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
        PushdownAutomaton automaton = PdaParser.parse(text);
        assertThat(automaton.getStates()).containsExactly("q0", "q1", "q2", "q3");
        assertThat(automaton.getAlphabet()).containsExactly("a", "b");
        assertThat(automaton.getStackAlphabet()).containsExactly("Z", "A");
        assertThat(automaton.getStart()).isEqualTo("q0");
        assertThat(automaton.getFinals()).containsExactly("q3");
        assertThat(automaton.getTransitions()).containsExactly(
            new PdaTransition("q0", "a", "Z", "q1", List.of("A", "Z")),
            new PdaTransition("q1", "a", "A", "q1", List.of("A", "A")),
            new PdaTransition("q1", "b", "A", "q2", List.of()),
            new PdaTransition("q2", "b", "A", "q2", List.of()),
            new PdaTransition("q2", PushdownAutomaton.LAMBDA, "Z", "q3", List.of()));
    }

    @Test
    void toleratesMissingEqualsSign() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z A
            start: q0
            final: q1
            transitions:
            q0 a Z q1 AZ
            """);
        assertThat(automaton.getTransitions())
            .containsExactly(new PdaTransition("q0", "a", "Z", "q1", List.of("A", "Z")));
    }

    @Test
    void acceptsEpsAliasForInput() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 eps Z = q1 Z
            """);
        assertThat(automaton.getTransitions())
            .containsExactly(new PdaTransition("q0", PushdownAutomaton.LAMBDA, "Z", "q1", List.of("Z")));
    }

    @Test
    void acceptsEpsilonAliasForPush() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z A
            start: q0
            final: q1
            transitions:
            q0 a A = q1 ε
            """);
        assertThat(automaton.getTransitions())
            .containsExactly(new PdaTransition("q0", "a", "A", "q1", List.of()));
    }

    @Test
    void splitsPushIntoStackSymbolsByLongestMatch() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0 q1 q2
            alphabet: a
            stack: Z AB A
            start: q0
            final: q2
            transitions:
            q0 a Z = q1 ABZ
            q1 a AB = q2 AZ
            """);
        assertThat(automaton.getTransitions()).containsExactly(
            new PdaTransition("q0", "a", "Z", "q1", List.of("AB", "Z")),
            new PdaTransition("q1", "a", "AB", "q2", List.of("A", "Z")));
    }

    @Test
    void allowsMissingFinalSection() {
        PushdownAutomaton automaton = PdaParser.parse("""
            states: q0
            alphabet: a
            stack: Z
            start: q0
            transitions:
            """);
        assertThat(automaton.getFinals()).isEmpty();
    }

    @Test
    void reportsMissingSections() {
        assertThatThrownBy(() -> PdaParser.parse("alphabet: a\nstack: Z\nstart: q0\n"))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("missing 'states:' declaration");
        assertThatThrownBy(() -> PdaParser.parse("states: q0\nstack: Z\nstart: q0\n"))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("missing 'alphabet:' declaration");
        assertThatThrownBy(() -> PdaParser.parse("states: q0\nalphabet: a\nstart: q0\n"))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("missing 'stack:' declaration");
        assertThatThrownBy(() -> PdaParser.parse("states: q0\nalphabet: a\nstack: Z\n"))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("missing 'start:' declaration");
    }

    @Test
    void reportsUnknownKey() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0
            alphabet: a
            stack: Z
            start: q0
            garbage: x
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 5")
            .hasMessageContaining("unknown key 'garbage'");
    }

    @Test
    void reportsUnknownStateWithLineNumber() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 a Z = q9 Z
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 7")
            .hasMessageContaining("unknown state 'q9'");
    }

    @Test
    void reportsUnknownInputSymbol() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 c Z = q1 Z
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 7")
            .hasMessageContaining("unknown input symbol 'c'");
    }

    @Test
    void reportsUnknownStackTop() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 a X = q1 Z
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 7")
            .hasMessageContaining("unknown stack symbol 'X'");
    }

    @Test
    void reportsUnknownPushSymbol() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z A
            start: q0
            final: q1
            transitions:
            q0 a Z = q1 AZQ
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 7")
            .hasMessageContaining("cannot split the stack replacement 'AZQ'")
            .hasMessageContaining("near position 3");
    }

    @Test
    void reportsTooFewTokens() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 a Z = q1
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 7")
            .hasMessageContaining("a transition must be 'state input stackTop = state push'");
    }

    @Test
    void reportsTooManyEqualsSigns() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 a Z = = q1 Z
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 7")
            .hasMessageContaining("at most one '=' separator");
    }

    @Test
    void reportsContentOutsideOfSections() {
        assertThatThrownBy(() -> PdaParser.parse("q0 a Z = q1 Z"))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 1")
            .hasMessageContaining("unexpected content outside of any section");
    }

    @Test
    void reportsStartStateNotDeclared() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0
            alphabet: a
            stack: Z
            start: q7
            final: q0
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("start state 'q7' is not declared");
    }

    @Test
    void reportsDuplicateSection() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0
            states: q1
            alphabet: a
            stack: Z
            start: q0
            """))
            .isInstanceOf(AutomatonFormatException.class)
            .hasMessageContaining("line 2")
            .hasMessageContaining("duplicate 'states:' section");
    }

    @Test
    void rejectsNonDeterministicDuplicateTransition() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 a Z = q1 Z
            q0 a Z = q1 λ
            """))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not deterministic")
            .hasMessageContaining("defined more than once");
    }

    @Test
    void rejectsNonDeterministicLambdaConflict() {
        assertThatThrownBy(() -> PdaParser.parse("""
            states: q0 q1
            alphabet: a
            stack: Z
            start: q0
            final: q1
            transitions:
            q0 λ Z = q1 Z
            q0 a Z = q1 Z
            """))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not deterministic")
            .hasMessageContaining("both a λ-transition and a transition on input 'a'");
    }
}
