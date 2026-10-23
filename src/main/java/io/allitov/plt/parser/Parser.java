package io.allitov.plt.parser;

import io.allitov.plt.tree.Node;
import io.allitov.plt.exception.SyntaxException;
import io.allitov.plt.token.Token;
import io.allitov.plt.token.TokenType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Parser {

    private static final String PRIMARY_EXPECTED = "number, id or '('";

    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("Token list must not be null");
        }
        this.tokens = List.copyOf(tokens);
    }

    public Node parse() {
        if (tokens.isEmpty()) {
            throw new SyntaxException(
                    "Error at position 0. Expected: " + PRIMARY_EXPECTED,
                    0,
                    new Node("S", List.of(new Node("error", List.of()))));
        }

        Node result = null;
        try {
            result = parseS();
            match(TokenType.EOF);
            return result;
        } catch (SyntaxException exception) {
            if (result != null) {
                throw exception.withPartialTree(result);
            }
            throw exception;
        }
    }

    Node parseS() {
        return sequence("S", this::parseE);
    }

    Node parseE() {
        return sequence("E", this::parseT, this::parseEPrime);
    }

    Node parseEPrime() {
        TokenType current = checkCurrentToken().type();
        if (current != TokenType.PLUS && current != TokenType.MINUS) {
            return new Node("E' (ε)", List.of());
        }

        return sequence("E'", () -> matchOperator(current), this::parseT, this::parseEPrime);
    }

    Node parseT() {
        return sequence("T", this::parseF, this::parseTPrime);
    }

    Node parseTPrime() {
        TokenType current = checkCurrentToken().type();
        if (current != TokenType.MULTIPLY && current != TokenType.DIVIDE) {
            return new Node("T' (ε)", List.of());
        }

        return sequence("T'", () -> matchOperator(current), this::parseF, this::parseTPrime);
    }

    Node parseF() {
        Token current = checkCurrentToken();
        if (current.type() == TokenType.PLUS || current.type() == TokenType.MINUS) {
            return sequence("F", () -> matchOperator(current.type()), this::parseG);
        }

        return sequence("F", this::parseG);
    }

    Node parseG() {
        Token current = checkCurrentToken();
        if (current.type() == TokenType.NUMBER || current.type() == TokenType.ID) {
            return new Node("G", List.of(consumeTerminal()));
        }
        if (current.type() == TokenType.LEFT_PAREN) {
            return sequence(
                    "G",
                    () -> matchOperator(TokenType.LEFT_PAREN),
                    this::parseS,
                    () -> matchOperator(TokenType.RIGHT_PAREN));
        }

        throw syntaxException(PRIMARY_EXPECTED)
                .withPartialTree(new Node("G", List.of(new Node("error", List.of()))));
    }

    private Node matchOperator(TokenType expected) {
        Node operator = Node.terminal(currentToken());
        match(expected);
        return operator;
    }

    private void match(TokenType expected) {
        Token current = currentToken();
        if (current.type() != expected) {
            throw syntaxException(tokenExpectation(expected));
        }
        position++;
    }

    private Node consumeTerminal() {
        Token token = currentToken();
        position++;
        return Node.terminal(token);
    }

    private Token currentToken() {
        if (position >= tokens.size()) {
            throw syntaxException(TokenType.EOF.displayName());
        }
        return tokens.get(position);
    }

    private Token checkCurrentToken() {
        Token token = currentToken();
        log.info("current token check for {}", token);
        return token;
    }

    private SyntaxException syntaxException(String expected) {
        int errorPosition = position < tokens.size()
                ? tokens.get(position).position()
                : tokens.isEmpty() ? 0 : tokens.getLast().position();
        String message = "Error at position %d. Expected: %s".formatted(errorPosition, expected);
        return new SyntaxException(message, errorPosition);
    }

    /**
     * Parses the children of the {@code label} node in order. When a child fails, its
     * partial (truncated) subtree is embedded into the tree, the first failure is remembered,
     * and parsing continues with the remaining children. This way the tree printed on error
     * stays complete: only the branch containing the error is truncated, while all the
     * branches parsed before and after the error are rendered as usual.
     */
    private Node sequence(String label, ParseStep... steps) {
        List<Node> children = new ArrayList<>();
        SyntaxException failure = null;
        for (ParseStep step : steps) {
            try {
                children.add(step.parse());
            } catch (SyntaxException exception) {
                children.add(exception.partialTree());
                if (failure == null) {
                    failure = exception;
                }
            }
        }

        if (failure != null) {
            throw failure.withPartialTree(new Node(label, children));
        }
        return new Node(label, children);
    }

    @FunctionalInterface
    private interface ParseStep {

        Node parse();
    }

    private String tokenExpectation(TokenType type) {
        return switch (type) {
            case PLUS, MINUS, MULTIPLY, DIVIDE, LEFT_PAREN, RIGHT_PAREN -> "'%s'".formatted(type.displayName());
            case EOF, NUMBER, ID -> type.displayName();
        };
    }
}
