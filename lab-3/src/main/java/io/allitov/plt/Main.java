package io.allitov.plt;

import io.allitov.plt.exception.AutomatonFormatException;
import io.allitov.plt.pda.PdaParser;
import io.allitov.plt.pda.PdaPrinter;
import io.allitov.plt.pda.PdaRunner;
import io.allitov.plt.pda.PushdownAutomaton;
import io.allitov.plt.pda.RunResult;
import io.allitov.plt.pda.Step;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Scanner;

@Slf4j
public class Main {

    private static final PdaRunner RUNNER = new PdaRunner();

    static void main(String[] args) {
        try {
            runSession(args);
        } catch (IOException | AutomatonFormatException | IllegalArgumentException exception) {
            log.error("Error: {}", exception.getMessage());
            System.exit(1);
        }
    }

    private static void runSession(String[] args) throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            PushdownAutomaton automaton = loadAutomaton(args, scanner);
            log.info("{}", PdaPrinter.summary(automaton));
            log.info("Enter strings to check. Use λ for the empty string; an empty line exits.");
            while (true) {
                log.info("Enter a string:");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String chain = scanner.nextLine().trim();
                if (chain.isEmpty()) {
                    break;
                }
                log.info("{}", check(automaton, chain));
            }
            log.info("Bye!");
        }
    }

    static PushdownAutomaton loadAutomaton(String[] args, Scanner scanner) throws IOException {
        String path = args.length > 0 ? args[0] : null;
        if (path == null) {
            log.info("Enter the path to the automaton description file:");
            path = scanner.nextLine().trim();
        }
        try {
            return PdaParser.parse(Files.readString(Path.of(path), StandardCharsets.UTF_8));
        } catch (NoSuchFileException exception) {
            throw new IOException("the automaton file '" + path + "' does not exist");
        } catch (InvalidPathException exception) {
            throw new IOException("invalid file path '" + path + "'");
        }
    }

    static String check(PushdownAutomaton automaton, String chain) {
        RunResult result = RUNNER.run(automaton, chain);
        StringBuilder report = new StringBuilder();
        report.append("Checking string: ").append(PdaPrinter.formatChain(chain)).append('\n');
        report.append("Start: ").append(PdaPrinter.format(result.initial())).append('\n');
        for (Step step : result.steps()) {
            report.append(PdaPrinter.formatStep(step)).append('\n');
        }
        if (result.accepted()) {
            report.append("Result: ACCEPTED - the string belongs to the language (final state: ")
                  .append(result.last().state()).append(')');
        } else {
            report.append("Result: REJECTED - the string does not belong to the language.").append('\n');
            report.append("Reason: ").append(result.reason());
        }
        return report.toString();
    }
}
