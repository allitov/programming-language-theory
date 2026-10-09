package io.allitov.plt.parser;

import io.allitov.plt.tree.Node;
import io.allitov.plt.exception.SyntaxException;
import io.allitov.plt.token.Token;
import io.allitov.plt.token.TokenType;

import java.util.ArrayList;
import java.util.List;

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
        Node expression = null;
        try {
            expression = parseE();
            return new Node("S", List.of(expression));
        } catch (SyntaxException exception) {
            throw raisePartial(exception, "S", new Node[]{expression});
        }
    }

    Node parseE() {
        Node term = null;
        Node expressionPrime = null;
        try {
            term = parseT();
            expressionPrime = parseEPrime();
            return new Node("E", List.of(term, expressionPrime));
        } catch (SyntaxException exception) {
            throw raisePartial(exception, "E", new Node[]{term, expressionPrime});
        }
    }

    Node parseEPrime() {
        TokenType current = currentToken().type();
        if (current != TokenType.PLUS && current != TokenType.MINUS) {
            return new Node("E' (ε)", List.of());
        }

        Node operator = null;
        Node term = null;
        Node tail = null;
        try {
            operator = Node.terminal(currentToken());
            match(current);
            term = parseT();
            tail = parseEPrime();
            return new Node("E'", List.of(operator, term, tail));
        } catch (SyntaxException exception) {
            throw raisePartial(exception, "E'", new Node[]{operator, term, tail});
        }
    }

    Node parseT() {
        Node factor = null;
        Node termPrime = null;
        try {
            factor = parseF();
            termPrime = parseTPrime();
            return new Node("T", List.of(factor, termPrime));
        } catch (SyntaxException exception) {
            throw raisePartial(exception, "T", new Node[]{factor, termPrime});
        }
    }

    Node parseTPrime() {
        TokenType current = currentToken().type();
        if (current != TokenType.MULTIPLY && current != TokenType.DIVIDE) {
            return new Node("T' (ε)", List.of());
        }

        Node operator = null;
        Node factor = null;
        Node tail = null;
        try {
            operator = Node.terminal(currentToken());
            match(current);
            factor = parseF();
            tail = parseTPrime();
            return new Node("T'", List.of(operator, factor, tail));
        } catch (SyntaxException exception) {
            throw raisePartial(exception, "T'", new Node[]{operator, factor, tail});
        }
    }

    Node parseF() {
        Token current = currentToken();
        if (current.type() == TokenType.NUMBER || current.type() == TokenType.ID) {
            return consumeTerminal();
        }
        if (current.type() == TokenType.LEFT_PAREN) {
            Node leftParen = Node.terminal(current);
            match(TokenType.LEFT_PAREN);
            Node expression = null;
            Node rightParen = null;
            try {
                expression = parseS();
                rightParen = Node.terminal(currentToken());
                match(TokenType.RIGHT_PAREN);
                return new Node("F", List.of(leftParen, expression, rightParen));
            } catch (SyntaxException exception) {
                throw raisePartial(exception, "F", new Node[]{leftParen, expression, rightParen});
            }
        }

        throw syntaxException(PRIMARY_EXPECTED)
                .withPartialTree(new Node("F", List.of(new Node("error", List.of()))));
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

    private SyntaxException syntaxException(String expected) {
        int errorPosition = position < tokens.size()
                ? tokens.get(position).position()
                : tokens.isEmpty() ? 0 : tokens.getLast().position();
        String message = "Error at position %d. Expected: %s".formatted(errorPosition, expected);
        return new SyntaxException(message, errorPosition);
    }

    private SyntaxException raisePartial(
            SyntaxException exception, String label, Node[] parsedChildren) {
        List<Node> children = new ArrayList<>();
        boolean failureAdded = false;
        for (Node child : parsedChildren) {
            if (child != null) {
                children.add(child);
            } else if (!failureAdded) {
                children.add(exception.partialTree());
                failureAdded = true;
            } else {
                break;
            }
        }

        return exception.withPartialTree(new Node(label, children));
    }

    private String tokenExpectation(TokenType type) {
        return switch (type) {
            case PLUS, MINUS, MULTIPLY, DIVIDE, LEFT_PAREN, RIGHT_PAREN -> "'%s'".formatted(type.displayName());
            case EOF, NUMBER, ID -> type.displayName();
        };
    }
}
