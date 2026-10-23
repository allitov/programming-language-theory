package io.allitov.plt;

import io.allitov.plt.exception.AutomatonFormatException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MainTest {

    @Test
    void buildsReportForNfaInput() {
        String input = """
            states: q0 q1 q2
            alphabet: 0 1
            start: q0
            final: q2
            transitions:
            q0 0 q0 q1
            q0 1 q0
            q1 1 q2
            """;
        String report = Main.process(input);
        assertThat(report)
            .contains("Deterministic: no")
            .contains("state 'q0' has 2 transitions on symbol '0'")
            .contains("After subset construction:")
            .contains("Minimal DFA (states: 3 -> 3)")
            .contains("{q0,q1} 1 {q0,q2}");
    }

    @Test
    void deterministicInputHasNoIntermediateStage() {
        String input = """
            states: q0 q1
            alphabet: a
            start: q0
            final: q1
            transitions:
            q0 a q1
            """;
        String report = Main.process(input);
        assertThat(report)
            .contains("Deterministic: yes")
            .contains("Minimal DFA (states: 2 -> 2)")
            .doesNotContain("subset construction");
    }

    @Test
    void rejectsBrokenInput() {
        assertThatThrownBy(() -> Main.process("nonsense"))
            .isInstanceOf(AutomatonFormatException.class);
    }
}
