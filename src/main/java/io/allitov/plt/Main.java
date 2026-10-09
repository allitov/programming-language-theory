package io.allitov.plt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main {

    static void main(String[] args) {
        if (args.length > 0) {
            for (String expression : args) {
                System.out.println(analyze(expression));
            }
            return;
        }

        System.out.println("Введите арифметическое выражение (exit — выход):");
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                String expression = line.trim();
                if (expression.equalsIgnoreCase("exit") || expression.equalsIgnoreCase("quit")) {
                    break;
                }
                if (!expression.isEmpty()) {
                    System.out.println(analyze(expression));
                }
            }
        } catch (IOException exception) {
            System.out.println("Ошибка чтения ввода: " + exception.getMessage());
        }
    }

    static String analyze(String input) {
        try {
            Node tree = new Parser(new Lexer(input).tokenize()).parse();
            return "Выражение корректно.\n" + TreePrinter.render(tree);
        } catch (LexicalException | SyntaxException exception) {
            return exception.getMessage();
        }
    }
}
