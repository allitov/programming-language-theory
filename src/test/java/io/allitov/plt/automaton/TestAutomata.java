package io.allitov.plt.automaton;

import java.util.List;
import java.util.Set;

/** Compact factory for building automata in tests. */
final class TestAutomata {

    private TestAutomata() {
    }

    static FiniteAutomaton automaton(Set<String> states,
                                     Set<String> alphabet,
                                     String start,
                                     Set<String> finals,
                                     Transition... transitions) {
        return new FiniteAutomaton(states, alphabet, start, finals, List.of(transitions));
    }

    /** NFA over {0, 1} accepting exactly the words that end with "01". */
    static FiniteAutomaton endsWith01Nfa() {
        return automaton(Set.of("q0", "q1", "q2"), Set.of("0", "1"), "q0", Set.of("q2"),
            new Transition("q0", "0", "q0"),
            new Transition("q0", "1", "q0"),
            new Transition("q0", "0", "q1"),
            new Transition("q1", "1", "q2"));
    }

    /** Already minimal DFA over {a, b} accepting exactly the words that end with "a". */
    static FiniteAutomaton endsWithA() {
        return automaton(Set.of("q0", "q1"), Set.of("a", "b"), "q0", Set.of("q1"),
            new Transition("q0", "a", "q1"),
            new Transition("q0", "b", "q0"),
            new Transition("q1", "a", "q1"),
            new Transition("q1", "b", "q0"));
    }
}
