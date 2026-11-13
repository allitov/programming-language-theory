package io.allitov.plt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import io.allitov.plt.pda.PdaParser;
import io.allitov.plt.pda.PushdownAutomaton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MainTest {

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

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;

    @AfterEach
    void restoreSystemStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    void runsInteractiveSession(@TempDir Path tempDir) throws Exception {
        Path automatonFile = tempDir.resolve("anbn.txt");
        Files.writeString(automatonFile, ANBN, StandardCharsets.UTF_8);
        String input = String.join("\n", automatonFile.toString(), "ab", "aab", "λ", "") + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        Main.main(new String[0]);

        String console = output.toString(StandardCharsets.UTF_8);
        assertThat(console)
            .contains("Enter the path to the automaton description file:")
            .contains("Automaton loaded:")
            .contains("States: q0 q1 q2 q3")
            .contains("q2 λ Z = q3 λ")
            .contains("Enter strings to check. Use λ for the empty string; an empty line exits.")
            .contains("Checking string: ab")
            .contains("Start: (q0, ab, Z)")
            .contains("1) (q0, ab, Z) -> (q1, b, AZ)")
            .contains("2) (q1, b, AZ) -> (q2, λ, Z)")
            .contains("3) (q2, λ, Z) -> (q3, λ, λ)")
            .contains("Result: ACCEPTED - the string belongs to the language (final state: q3)")
            .contains("Checking string: aab")
            .contains("3) (q1, b, AAZ) -> (q2, λ, AZ)")
            .contains("Result: REJECTED - the string does not belong to the language.")
            .contains("Reason: the input has been fully consumed but the automaton stopped in state 'q2', "
                + "which is not a final state (final states: [q3])")
            .contains("Checking string: λ")
            .contains("Reason: the input has been fully consumed but the automaton stopped in state 'q0', "
                + "which is not a final state (final states: [q3])")
            .contains("Bye!");
    }

    @Test
    void readsAutomatonFromCommandLineArgument(@TempDir Path tempDir) throws Exception {
        Path automatonFile = tempDir.resolve("anbn.txt");
        Files.writeString(automatonFile, ANBN, StandardCharsets.UTF_8);
        System.setIn(new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8)));
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        Main.main(new String[]{automatonFile.toString()});

        String console = output.toString(StandardCharsets.UTF_8);
        assertThat(console)
            .doesNotContain("Enter the path")
            .contains("Automaton loaded:")
            .contains("Bye!");
    }

    @Test
    void checkProducesTraceAndVerdict() {
        PushdownAutomaton automaton = PdaParser.parse(ANBN);
        String accepted = Main.check(automaton, "aabb");
        assertThat(accepted)
            .contains("Checking string: aabb")
            .contains("Start: (q0, aabb, Z)")
            .contains("1) (q0, aabb, Z) -> (q1, abb, AZ)")
            .contains("5) (q2, λ, Z) -> (q3, λ, λ)")
            .contains("Result: ACCEPTED - the string belongs to the language (final state: q3)");
        String rejected = Main.check(automaton, "b");
        assertThat(rejected)
            .contains("Result: REJECTED - the string does not belong to the language.")
            .contains("Reason: no transition is defined for the configuration (q0, b, Z): "
                + "the automaton got stuck while reading symbol 'b' (position 1)");
    }

    @Test
    void loadAutomatonReportsMissingFile() {
        assertThatThrownBy(() -> Main.loadAutomaton(new String[]{"missing-automaton.txt"}, new Scanner("")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("the automaton file 'missing-automaton.txt' does not exist");
    }
}
