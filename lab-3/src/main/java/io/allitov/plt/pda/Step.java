package io.allitov.plt.pda;

public record Step(int number, Configuration before, Configuration after, PdaTransition transition) {
}
