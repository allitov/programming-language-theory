package io.allitov.plt.tree;

import io.allitov.plt.token.Token;

import java.util.List;

public record Node(String label, List<Node> children) {

    public Node {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Node label must not be blank");
        }
        if (children == null) {
            throw new IllegalArgumentException("Child node list must not be null");
        }
        children = List.copyOf(children);
    }

    public static Node terminal(Token token) {
        return new Node(token.type().displayName() + " '" + token.value() + "'", List.of());
    }
}
