package io.allitov.plt;

public final class TreePrinter {

    private TreePrinter() {
    }

    public static String render(Node root) {
        if (root == null) {
            throw new IllegalArgumentException("Корень дерева не должен быть null");
        }

        StringBuilder output = new StringBuilder();
        render(root, "", true, true, output);
        return output.toString();
    }

    public static void print(Node root) {
        System.out.print(render(root));
    }

    private static void render(Node node, String prefix, boolean isLast, boolean isRoot, StringBuilder output) {
        if (isRoot) {
            output.append(node.label()).append('\n');
        } else {
            output.append(prefix)
                    .append(isLast ? "└── " : "├── ")
                    .append(node.label())
                    .append('\n');
        }

        for (int index = 0; index < node.children().size(); index++) {
            Node child = node.children().get(index);
            boolean last = index == node.children().size() - 1;
            String childPrefix = isRoot ? "" : prefix + (isLast ? "    " : "│   ");
            render(child, childPrefix, last, false, output);
        }
    }
}
