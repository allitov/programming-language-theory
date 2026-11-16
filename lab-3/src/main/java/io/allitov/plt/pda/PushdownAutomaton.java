package io.allitov.plt.pda;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.ToString;
import lombok.Value;

@Value
@ToString(onlyExplicitlyIncluded = true)
public class PushdownAutomaton {

    public static final String LAMBDA = "λ";

    public static final String EPSILON = "ε";

    public static final String INITIAL_STACK = "Z";

    @ToString.Include
    Set<String> states;

    @ToString.Include
    Set<String> alphabet;

    @ToString.Include
    Set<String> stackAlphabet;

    @ToString.Include
    String start;

    @ToString.Include
    Set<String> finals;

    Set<PdaTransition> transitions;

    Map<TransitionKey, PdaTransition> transitionsByKey;

    public PushdownAutomaton(Set<String> states,
                             Set<String> alphabet,
                             Set<String> stackAlphabet,
                             String start,
                             Set<String> finals,
                             Collection<PdaTransition> transitions) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(alphabet, "alphabet");
        Objects.requireNonNull(stackAlphabet, "stackAlphabet");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(finals, "finals");
        Objects.requireNonNull(transitions, "transitions");
        Set<String> stateCopy = new LinkedHashSet<>(states);
        if (stateCopy.isEmpty()) {
            throw new IllegalArgumentException("automaton must declare at least one state");
        }
        if (!stateCopy.contains(start)) {
            throw new IllegalArgumentException("start state '" + start + "' is not declared in states");
        }
        for (String finalState : finals) {
            if (!stateCopy.contains(finalState)) {
                throw new IllegalArgumentException("final state '" + finalState + "' is not declared in states");
            }
        }
        Set<String> alphabetCopy = new LinkedHashSet<>();
        for (String symbol : alphabet) {
            alphabetCopy.add(requirePlainSymbol(symbol, "alphabet"));
        }
        Set<String> stackCopy = new LinkedHashSet<>();
        for (String symbol : stackAlphabet) {
            stackCopy.add(requirePlainSymbol(symbol, "stack alphabet"));
        }
        if (!stackCopy.contains(INITIAL_STACK)) {
            throw new IllegalArgumentException("the stack alphabet must contain the initial stack symbol '" + INITIAL_STACK + "'");
        }
        Set<PdaTransition> transitionCopy = new LinkedHashSet<>();
        for (PdaTransition transition : transitions) {
            Objects.requireNonNull(transition, "transition");
            if (!stateCopy.contains(transition.from())) {
                throw new IllegalArgumentException("transition source '" + transition.from() + "' is not declared in states");
            }
            if (!stateCopy.contains(transition.to())) {
                throw new IllegalArgumentException("transition target '" + transition.to() + "' is not declared in states");
            }
            if (!LAMBDA.equals(transition.input()) && !alphabetCopy.contains(transition.input())) {
                throw new IllegalArgumentException("transition input '" + transition.input() + "' is not declared in the alphabet");
            }
            if (!stackCopy.contains(transition.top())) {
                throw new IllegalArgumentException("transition stack top '" + transition.top() + "' is not declared in the stack alphabet");
            }
            for (String pushed : transition.push()) {
                if (!stackCopy.contains(pushed)) {
                    throw new IllegalArgumentException("transition push symbol '" + pushed + "' is not declared in the stack alphabet");
                }
            }
            transitionCopy.add(transition);
        }
        requireUniqueTransitions(transitionCopy);
        this.states = Collections.unmodifiableSet(stateCopy);
        this.alphabet = Collections.unmodifiableSet(alphabetCopy);
        this.stackAlphabet = Collections.unmodifiableSet(stackCopy);
        this.start = start;
        this.finals = Collections.unmodifiableSet(new LinkedHashSet<>(finals));
        this.transitions = Collections.unmodifiableSet(transitionCopy);
        this.transitionsByKey = index(transitionCopy);
    }

    public Optional<PdaTransition> transition(String state, String input, String stackTop) {
        return Optional.ofNullable(transitionsByKey.get(new TransitionKey(state, input, stackTop)));
    }

    private static String requirePlainSymbol(String symbol, String where) {
        Objects.requireNonNull(symbol, "symbol");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException(where + " symbols must not be blank");
        }
        if (LAMBDA.equals(symbol) || EPSILON.equals(symbol)) {
            throw new IllegalArgumentException("'" + symbol + "' denotes emptiness and must not be declared as a symbol of the " + where);
        }
        return symbol;
    }

    private static void requireUniqueTransitions(Set<PdaTransition> transitions) {
        Set<TransitionKey> seen = new HashSet<>();
        for (PdaTransition transition : transitions) {
            TransitionKey key = new TransitionKey(transition.from(), transition.input(), transition.top());
            if (!seen.add(key)) {
                throw new IllegalArgumentException("the automaton is not deterministic: the transition (" + transition.from()
                    + ", " + transition.input() + ", " + transition.top() + ") is defined more than once");
            }
        }
    }

    private static Map<TransitionKey, PdaTransition> index(Set<PdaTransition> transitions) {
        Map<TransitionKey, PdaTransition> index = new LinkedHashMap<>();
        for (PdaTransition transition : transitions) {
            index.put(new TransitionKey(transition.from(), transition.input(), transition.top()), transition);
        }
        return Collections.unmodifiableMap(index);
    }

    private record TransitionKey(String state, String input, String stackTop) {
    }
}
