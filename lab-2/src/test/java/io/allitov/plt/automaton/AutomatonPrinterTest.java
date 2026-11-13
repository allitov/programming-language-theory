package io.allitov.plt.automaton;

import org.junit.jupiter.api.Test;

import static io.allitov.plt.automaton.TestAutomata.endsWithA;
import static org.assertj.core.api.Assertions.assertThat;

class AutomatonPrinterTest {

    private static final String SAMPLE = """
        states: q0 q1 q2
        alphabet: a b
        start: q0
        final: q1 q2
        transitions:
        q0 a q1
        q0 b q0
        q1 a q2
        q1 b q2
        q2 eps q0
        """;

    @Test
    void printedAutomatonIsParseableBackAndEqual() {
        FiniteAutomaton automaton = AutomatonParser.parse(SAMPLE);
        FiniteAutomaton reparsed = AutomatonParser.parse(AutomatonPrinter.print(automaton));
        assertThat(reparsed).isEqualTo(automaton);
    }

    @Test
    void dotMarksFinalStatesAndStart() {
        String dot = AutomatonPrinter.toDot(endsWithA());
        assertThat(dot).startsWith("digraph automaton");
        assertThat(dot).contains("__start -> \"q0\"");
        assertThat(dot).contains("\"q1\" [shape = doublecircle]");
        assertThat(dot).contains("[label = \"a\"]");
    }
}
