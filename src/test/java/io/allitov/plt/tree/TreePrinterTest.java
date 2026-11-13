package io.allitov.plt.tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

class TreePrinterTest {

    @Test
    void rendersRootWithoutChildren() {
        assertThat(TreePrinter.render(new Node("S", List.of()))).isEqualTo("S\n");
    }

    @Test
    void rendersNestedTreeWithBranchConnectors() {
        Node root = new Node("S", List.of(
                new Node("E", List.of(new Node("T", List.of()))),
                new Node("EOF", List.of())));

        assertThat(TreePrinter.render(root)).isEqualTo("""
                S
                ├── E
                │   └── T
                └── EOF
                """);
    }

    @Test
    void rejectsNullRoot() {
        assertThatIllegalArgumentException().isThrownBy(() -> TreePrinter.render(null))
                .withMessage("Tree root must not be null");
    }
}
