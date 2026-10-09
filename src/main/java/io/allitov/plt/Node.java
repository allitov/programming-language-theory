package io.allitov.plt;

import java.util.List;

public record Node(String label, List<Node> children) {

    public Node {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Метка узла не должна быть пустой");
        }
        if (children == null) {
            throw new IllegalArgumentException("Список дочерних узлов не должен быть null");
        }
        children = List.copyOf(children);
    }

    public static Node terminal(Token token) {
        return new Node(token.type().displayName() + " '" + token.value() + "'", List.of());
    }
}
