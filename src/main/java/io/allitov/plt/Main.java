package io.allitov.plt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.allitov.plt.automaton.AutomatonMinimizer;
import io.allitov.plt.automaton.AutomatonParser;
import io.allitov.plt.automaton.AutomatonPrinter;
import io.allitov.plt.automaton.DeterminismReport;
import io.allitov.plt.automaton.FiniteAutomaton;
import io.allitov.plt.automaton.MinimizationResult;
import io.allitov.plt.exception.AutomatonFormatException;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry point: reads an automaton from a file (the first argument) or from stdin,
 * minimizes it and prints the report: the determinism check, the intermediate DFA
 * (for a non-deterministic input) and the minimal DFA.
 */
@Slf4j
public class Main {

    static void main(String[] args) {
        try {
            String input = args.length > 0
                ? Files.readString(Path.of(args[0]), StandardCharsets.UTF_8)
                : new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            log.info("input source: {}", args.length > 0 ? args[0] : "stdin");
            System.out.print(process(input));
        } catch (IOException | AutomatonFormatException | IllegalArgumentException exception) {
            log.error("minimization failed: {}", exception.getMessage());
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    /** Runs the full pipeline and renders the human-readable report. */
    static String process(String input) {
        FiniteAutomaton original = AutomatonParser.parse(input);
        MinimizationResult result = new AutomatonMinimizer().minimize(original);
        DeterminismReport determinism = result.determinism();
        StringBuilder report = new StringBuilder();
        report.append("Input automaton:\n").append(AutomatonPrinter.print(original)).append('\n');
        report.append("Deterministic: ").append(determinism.deterministic() ? "yes" : "no").append('\n');
        for (String violation : determinism.violations()) {
            report.append("  ! ").append(violation).append('\n');
        }
        if (!determinism.deterministic()) {
            report.append('\n')
                  .append("After subset construction:\n")
                  .append(AutomatonPrinter.print(result.determinized()))
                  .append('\n');
        }
        report.append('\n')
              .append("Minimal DFA (states: ").append(result.determinized().getStates().size())
              .append(" -> ").append(result.minimized().getStates().size()).append("):\n")
              .append(AutomatonPrinter.print(result.minimized()));
        return report.toString();
    }
}
