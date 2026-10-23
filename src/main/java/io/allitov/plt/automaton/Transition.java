package io.allitov.plt.automaton;

/**
 * A single automaton transition: the {@code from} state reads {@code symbol}
 * and moves to the {@code to} state.
 *
 * @param from   source state
 * @param symbol symbol consumed by the transition, may be {@link FiniteAutomaton#EPSILON}
 * @param to     target state
 */
public record Transition(String from, String symbol, String to) {
}
