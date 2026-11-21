package io.allitov.plt;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import io.allitov.plt.console.Console;
import io.allitov.plt.console.SystemConsole;
import io.allitov.plt.exception.ConfigurationException;
import io.allitov.plt.exception.TranslationException;
import io.allitov.plt.transducer.Lexeme;
import io.allitov.plt.transducer.Lexer;
import io.allitov.plt.transducer.Step;
import io.allitov.plt.transducer.Transducer;
import io.allitov.plt.transducer.TransducerParser;
import io.allitov.plt.transducer.TransducerPrinter;
import io.allitov.plt.transducer.TransducerRunner;
import io.allitov.plt.transducer.TranslationResult;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Console entry point: loads a pushdown transducer from a json file, prints its summary and then
 * translates expression lines read from stdin, printing every tick. An empty line or the
 * {@code exit} command finishes the session; an error while translating one line does not stop it.
 */
@Slf4j
@UtilityClass
public class Main {

    private static final TransducerRunner RUNNER = new TransducerRunner();

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IOException | ConfigurationException | IllegalArgumentException exception) {
            log.error("Error: {}", exception.getMessage());
            System.exit(1);
        }
    }

    private void run(String[] args) throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            Transducer transducer = loadTransducer(args, scanner);
            log.info("{}", TransducerPrinter.summary(transducer));
            runSession(new SystemConsole(), scanner, transducer);
        }
    }

    static Transducer loadTransducer(String[] args, Scanner scanner) throws IOException {
        String path = args.length > 0 ? args[0] : null;
        if (path == null) {
            log.info("Enter the path to the transducer json configuration file:");
            path = scanner.nextLine().trim();
        }
        try {
            return TransducerParser.parse(Path.of(path));
        } catch (NoSuchFileException _) {
            throw new IOException("the transducer file '" + path + "' does not exist");
        } catch (InvalidPathException _) {
            throw new IOException("invalid transducer file path '" + path + "'");
        }
    }

    static void runSession(Console console, Scanner scanner, Transducer transducer) {
        console.println("The transducer translates arithmetic expressions into reverse polish notation (RPN).");
        console.println("An empty line or the 'exit' command finishes the session.");
        while (true) {
            console.println("Enter an expression:");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty() || "exit".equalsIgnoreCase(line)) {
                break;
            }
            translate(console, transducer, line);
        }
        console.println("Bye!");
    }

    static void translate(Console console, Transducer transducer, String line) {
        try {
            List<Lexeme> lexemes = Lexer.tokenize(line);
            TranslationResult result = RUNNER.run(transducer, lexemes);
            console.println("Translating: " + line);
            for (Step step : result.steps()) {
                String stepLine = TransducerPrinter.formatStep(step);
                console.println(stepLine);
                log.info("tick {}: state '{}' stack '{}' output '{}'", step.number(), step.after().state(),
                    step.after().stack(), step.after().output());
            }
            String rpn = result.output().strip();
            console.println("RPN: " + rpn);
            console.println("ACCEPTED: the expression has been translated (final state '"
                + result.last().state() + "').");
            log.info("translation accepted: input '{}' -> rpn '{}'", line, rpn);
        } catch (TranslationException exception) {
            console.println("Error: " + exception.getMessage());
            log.warn("translation rejected: {}", exception.getMessage());
        }
    }
}
