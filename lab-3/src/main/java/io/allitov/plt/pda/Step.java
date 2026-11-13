package io.allitov.plt.pda;

/**
 * One executed move of the automaton: the configuration before the move, the configuration after it
 * and the transition that was applied. The number is 1-based and matches the order of execution.
 */
public record Step(int number, Configuration before, Configuration after, PdaTransition transition) {
}
