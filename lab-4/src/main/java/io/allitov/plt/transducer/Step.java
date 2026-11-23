package io.allitov.plt.transducer;

public record Step(
        int number,
        Configuration before,
        Configuration after,
        Transition transition,
        boolean inputConsumed
) {
}
