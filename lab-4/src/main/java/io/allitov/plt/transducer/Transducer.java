package io.allitov.plt.transducer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.Value;

/**
 * Immutable description of a pushdown transducer {@code P = (Q, V, Z, V', δ, q0, z0, F)}.
 * States and symbols are string identifiers loaded from a json configuration, not hard-coded enums.
 */
@Value
public class Transducer {

    /** Denotes the empty chain (input, stack, output tape). */
    public static final String LAMBDA = "λ";

    /** Alias of the empty chain that must not be declared as a real symbol. */
    public static final String EPSILON = "ε";

    /** The input chain end marker appended by the lexical analyzer. */
    public static final String INPUT_END = "⊥";

    Set<String> states;

    Set<String> inputAlphabet;

    Set<String> stackAlphabet;

    Set<String> outputAlphabet;

    String startState;

    String startStackSymbol;

    Set<String> finalStates;

    Set<Transition> transitions;

    Map<TransitionKey, Transition> transitionsByKey;

    public Transducer(Set<String> states,
                      Set<String> inputAlphabet,
                      Set<String> stackAlphabet,
                      Set<String> outputAlphabet,
                      String startState,
                      String startStackSymbol,
                      Set<String> finalStates,
                      Collection<Transition> transitions) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(inputAlphabet, "inputAlphabet");
        Objects.requireNonNull(stackAlphabet, "stackAlphabet");
        Objects.requireNonNull(outputAlphabet, "outputAlphabet");
        Objects.requireNonNull(startState, "startState");
        Objects.requireNonNull(startStackSymbol, "startStackSymbol");
        Objects.requireNonNull(finalStates, "finalStates");
        Objects.requireNonNull(transitions, "transitions");
        Set<String> stateCopy = new LinkedHashSet<>(states);
        if (stateCopy.isEmpty()) {
            throw new IllegalArgumentException("the transducer must declare at least one state");
        }
        if (!stateCopy.contains(startState)) {
            throw new IllegalArgumentException("the start state '" + startState + "' is not declared in states");
        }
        for (String finalState : finalStates) {
            if (!stateCopy.contains(finalState)) {
                throw new IllegalArgumentException("the final state '" + finalState + "' is not declared in states");
            }
        }
        Set<String> inputCopy = new LinkedHashSet<>();
        for (String symbol : inputAlphabet) {
            inputCopy.add(requirePlainSymbol(symbol, "input alphabet"));
        }
        Set<String> stackCopy = new LinkedHashSet<>();
        for (String symbol : stackAlphabet) {
            stackCopy.add(requirePlainSymbol(symbol, "stack alphabet"));
        }
        if (!stackCopy.contains(startStackSymbol)) {
            throw new IllegalArgumentException("the start stack symbol '" + startStackSymbol
                + "' is not declared in the stack alphabet");
        }
        Set<String> outputCopy = new LinkedHashSet<>();
        for (String symbol : outputAlphabet) {
            outputCopy.add(requireOutputSymbol(symbol));
        }
        Set<Transition> transitionCopy = new LinkedHashSet<>();
        for (Transition transition : transitions) {
            Objects.requireNonNull(transition, "transition");
            if (!stateCopy.contains(transition.state())) {
                throw new IllegalArgumentException("transition source state '" + transition.state()
                    + "' is not declared in states");
            }
            if (!stateCopy.contains(transition.newState())) {
                throw new IllegalArgumentException("transition target state '" + transition.newState()
                    + "' is not declared in states");
            }
            if (!LAMBDA.equals(transition.input()) && !inputCopy.contains(transition.input())) {
                throw new IllegalArgumentException("transition input '" + transition.input()
                    + "' is not declared in the input alphabet");
            }
            if (!stackCopy.contains(transition.stackTop())) {
                throw new IllegalArgumentException("transition stack top '" + transition.stackTop()
                    + "' is not declared in the stack alphabet");
            }
            for (String pushed : transition.push()) {
                if (!stackCopy.contains(pushed)) {
                    throw new IllegalArgumentException("transition push symbol '" + pushed
                        + "' is not declared in the stack alphabet");
                }
            }
            for (String token : transition.output()) {
                if (!outputCopy.contains(token)) {
                    throw new IllegalArgumentException("transition output token '" + token
                        + "' is not declared in the output alphabet");
                }
            }
            transitionCopy.add(transition);
        }
        requireDeterministic(transitionCopy);
        this.states = Collections.unmodifiableSet(stateCopy);
        this.inputAlphabet = Collections.unmodifiableSet(inputCopy);
        this.stackAlphabet = Collections.unmodifiableSet(stackCopy);
        this.outputAlphabet = Collections.unmodifiableSet(outputCopy);
        this.startState = startState;
        this.startStackSymbol = startStackSymbol;
        this.finalStates = Collections.unmodifiableSet(new LinkedHashSet<>(finalStates));
        this.transitions = Collections.unmodifiableSet(transitionCopy);
        this.transitionsByKey = index(transitionCopy);
    }

    /** Returns the rule {@code δ(state, input, stackTop)} if it is defined. */
    public Optional<Transition> transition(String state, String input, String stackTop) {
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

    /**
     * Unlike the input and stack alphabets, the output alphabet legitimately contains the space
     * symbol (it separates lexemes on the output tape), so only empty strings are rejected there.
     */
    private static String requireOutputSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol");
        if (symbol.isEmpty()) {
            throw new IllegalArgumentException("output alphabet symbols must not be empty");
        }
        if (LAMBDA.equals(symbol) || EPSILON.equals(symbol)) {
            throw new IllegalArgumentException("'" + symbol + "' denotes emptiness and must not be declared as a symbol of the output alphabet");
        }
        return symbol;
    }

    /**
     * Verifies the determinism requirements: at most one rule per triple {@code (q, a, z)} and a
     * λ-rule for a pair {@code (q, z)} is allowed only when no rule by a real input symbol exists
     * for the same pair.
     */
    private static void requireDeterministic(Set<Transition> transitions) {
        Set<TransitionKey> exact = new HashSet<>();
        Map<TransitionKey, String> lambdaByPair = new HashMap<>();
        Map<TransitionKey, String> realByPair = new HashMap<>();
        for (Transition transition : transitions) {
            TransitionKey key = new TransitionKey(transition.state(), transition.input(), transition.stackTop());
            if (!exact.add(key)) {
                throw new IllegalArgumentException("the transducer is not deterministic: the transition ("
                    + transition.state() + ", " + transition.input() + ", " + transition.stackTop()
                    + ") is defined more than once");
            }
            TransitionKey pair = new TransitionKey(transition.state(), "", transition.stackTop());
            if (LAMBDA.equals(transition.input())) {
                lambdaByPair.merge(pair, transition.newState(), (first, _) -> first);
            } else {
                realByPair.merge(pair, transition.newState(), (first, _) -> first);
            }
        }
        for (TransitionKey pair : lambdaByPair.keySet()) {
            if (realByPair.containsKey(pair)) {
                throw new IllegalArgumentException("the transducer is not deterministic: a λ-transition for the pair ("
                    + pair.state() + ", " + pair.stackTop() + ") conflicts with a transition by a real input symbol");
            }
        }
    }

    private static Map<TransitionKey, Transition> index(Set<Transition> transitions) {
        Map<TransitionKey, Transition> index = new HashMap<>();
        for (Transition transition : transitions) {
            index.put(new TransitionKey(transition.state(), transition.input(), transition.stackTop()), transition);
        }
        return Collections.unmodifiableMap(index);
    }

    record TransitionKey(String state, String input, String stackTop) {
    }
}
