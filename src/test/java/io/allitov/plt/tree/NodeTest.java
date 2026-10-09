package io.allitov.plt.tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;

import io.allitov.plt.token.Token;
import io.allitov.plt.token.TokenType;
import org.junit.jupiter.api.Test;

class NodeTest {

    @Test
    void storesImmutableNodeParts() {
        List<Node> children = new ArrayList<>();
        children.add(new Node("F", List.of()));
        Node node = new Node("T", children);

        children.clear();

        assertThat(node.label()).isEqualTo("T");
        assertThat(node.children()).containsExactly(new Node("F", List.of()));
    }

    @Test
    void createsTerminalFromToken() {
        Node node = Node.terminal(new Token(TokenType.NUMBER, "42", 0));

        assertThat(node.label()).isEqualTo("number '42'");
        assertThat(node.children()).isEmpty();
    }

    @Test
    void rejectsInvalidNodeParts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Node(" ", List.of()))
                .withMessage("Node label must not be blank");
        assertThatIllegalArgumentException().isThrownBy(() -> new Node("T", null))
                .withMessage("Child node list must not be null");
    }
}
