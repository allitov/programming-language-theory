package io.allitov.plt.exception;

import io.allitov.plt.tree.Node;

import java.util.List;

public class SyntaxException extends RuntimeException {

    private final int position;
    private final Node partialTree;

    public SyntaxException(String message, int position) {
        this(message, position, new Node("error", List.of()));
    }

    public SyntaxException(String message, int position, Node partialTree) {
        super(message);
        if (partialTree == null) {
            throw new IllegalArgumentException("Partial parse tree must not be null");
        }
        this.position = position;
        this.partialTree = partialTree;
    }

    public int position() {
        return position;
    }

    public Node partialTree() {
        return partialTree;
    }

    public SyntaxException withPartialTree(Node partialTree) {
        return new SyntaxException(getMessage(), position, partialTree);
    }
}
