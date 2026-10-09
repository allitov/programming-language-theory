package io.allitov.plt;

import io.allitov.plt.exception.LexicalException;
import io.allitov.plt.exception.SyntaxException;
import io.allitov.plt.lexer.Lexer;
import io.allitov.plt.parser.Parser;
import io.allitov.plt.tree.Node;
import io.allitov.plt.tree.TreePrinter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
public class Main {

    static void main(String[] args) {
        if (args.length > 0) {
            log.info(analyze(String.join(" ", args)));
            return;
        }

        log.info("Enter an arithmetic expression (exit to quit):");
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                String expression = line.trim();
                if (expression.equalsIgnoreCase("exit") || expression.equalsIgnoreCase("quit")) {
                    break;
                }
                if (!expression.isEmpty()) {
                    log.info(analyze(expression));
                }
            }
        } catch (IOException exception) {
            log.error("Input reading error: {}", exception.getMessage());
        }
    }

    static String analyze(String input) {
        try {
            Node tree = new Parser(new Lexer(input).tokenize()).parse();
            return "Expression is valid.\n" + TreePrinter.render(tree);
        } catch (LexicalException | SyntaxException exception) {
            return exception.getMessage();
        }
    }
}
