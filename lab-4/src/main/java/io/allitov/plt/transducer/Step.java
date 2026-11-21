package io.allitov.plt.transducer;

/**
 * One tick of the transducer: {@code before ⊢ after} by the rule {@code transition};
 * {@code inputConsumed} states whether the tick read a real input symbol (false for λ-transitions).
 */
public record Step(int number, Configuration before, Configuration after, Transition transition,
                   boolean inputConsumed) {
}
