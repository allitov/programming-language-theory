package io.allitov.plt;

import java.util.List;
import java.util.Set;

public class Parser {

    private static final String PRIMARY_EXPECTED = "number, id или '('";
    private static final Set<TokenType> PRIMARY_TYPES = Set.of(
            TokenType.NUMBER, TokenType.ID, TokenType.LEFT_PAREN);

    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("Список токенов не должен быть null");
        }
        this.tokens = List.copyOf(tokens);
    }

    public Node parse() {
        if (tokens.isEmpty()) {
            throw new SyntaxException("Ошибка в позиции 0. Ожидалось: " + PRIMARY_EXPECTED, 0);
        }

        Node result = parseS();
        match(TokenType.EOF);
        return result;
    }

    Node parseS() {
        return new Node("S", List.of(parseE()));
    }

    Node parseE() {
        Node term = parseT();
        Node expressionPrime = parseEPrime();
        return new Node("E", List.of(term, expressionPrime));
    }

    Node parseEPrime() {
        TokenType current = currentToken().type();
        if (current != TokenType.PLUS && current != TokenType.MINUS) {
            return new Node("E' (ε)", List.of());
        }

        Node operator = Node.terminal(currentToken());
        match(current);
        Node term = parseT();
        Node tail = parseEPrime();
        return new Node("E'", List.of(operator, term, tail));
    }

    Node parseT() {
        Node factor = parseF();
        Node termPrime = parseTPrime();
        return new Node("T", List.of(factor, termPrime));
    }

    Node parseTPrime() {
        TokenType current = currentToken().type();
        if (current != TokenType.MULTIPLY && current != TokenType.DIVIDE) {
            return new Node("T' (ε)", List.of());
        }

        Node operator = Node.terminal(currentToken());
        match(current);
        Node factor = parseF();
        Node tail = parseTPrime();
        return new Node("T'", List.of(operator, factor, tail));
    }

    Node parseF() {
        Token current = currentToken();
        if (current.type() == TokenType.NUMBER || current.type() == TokenType.ID) {
            return consumeTerminal();
        }
        if (current.type() == TokenType.LEFT_PAREN) {
            Node leftParen = Node.terminal(current);
            match(TokenType.LEFT_PAREN);
            Node expression = parseS();
            Node rightParen = Node.terminal(currentToken());
            match(TokenType.RIGHT_PAREN);
            return new Node("F", List.of(leftParen, expression, rightParen));
        }

        throw syntaxException(PRIMARY_EXPECTED);
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
        String message = "Ошибка в позиции %d. Ожидалось: %s".formatted(errorPosition, expected);
        return new SyntaxException(message, errorPosition);
    }

    private String tokenExpectation(TokenType type) {
        return switch (type) {
            case PLUS, MINUS, MULTIPLY, DIVIDE, LEFT_PAREN, RIGHT_PAREN -> "'" + type.displayName() + "'";
            case EOF -> type.displayName();
            case NUMBER, ID -> type.displayName();
        };
    }
}
