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

/**
 * A deterministic pushdown automaton (DPDA).
 *
 * <p>The automaton always starts with the initial stack symbol {@code Z} as the only stack entry.
 * The transition function maps a triple (state, input symbol, stack top) to exactly one move;
 * a λ input means a move that does not consume any input symbol. Determinism is checked on
 * construction: for every triple at most one transition may exist, and a λ move must not
 * coexist with a move that reads a real symbol for the same (state, stack top) pair.</p>
 */
@Value
@ToString(onlyExplicitlyIncluded = true)
public class PushdownAutomaton {

    /** Denotes the empty string (an empty remaining input) and the empty stack. */
    public static final String LAMBDA = "λ";

    /** Accepted alias of {@link #LAMBDA} in transition descriptions. */
    public static final String EPSILON = "ε";

    /** The initial (bottom) stack symbol which is always on the stack when the run starts. */
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
        requireDeterminism(transitionCopy);
        this.states = Collections.unmodifiableSet(stateCopy);
        this.alphabet = Collections.unmodifiableSet(alphabetCopy);
        this.stackAlphabet = Collections.unmodifiableSet(stackCopy);
        this.start = start;
        this.finals = Collections.unmodifiableSet(new LinkedHashSet<>(finals));
        this.transitions = Collections.unmodifiableSet(transitionCopy);
        this.transitionsByKey = index(transitionCopy);
    }

    /**
     * Returns the only transition applicable in the configuration (state, input, stackTop), if any.
     */
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

    private static void requireDeterminism(Set<PdaTransition> transitions) {
        Set<TransitionKey> seen = new HashSet<>();
        Map<String, Map<String, Set<String>>> inputsByStateTop = new LinkedHashMap<>();
        for (PdaTransition transition : transitions) {
            TransitionKey key = new TransitionKey(transition.from(), transition.input(), transition.top());
            if (!seen.add(key)) {
                throw new IllegalArgumentException("the automaton is not deterministic: the transition (" + transition.from()
                    + ", " + transition.input() + ", " + transition.top() + ") is defined more than once");
            }
            inputsByStateTop.computeIfAbsent(transition.from(), ignored -> new LinkedHashMap<>())
                            .computeIfAbsent(transition.top(), ignored -> new LinkedHashSet<>())
                            .add(transition.input());
        }
        for (Map.Entry<String, Map<String, Set<String>>> stateEntry : inputsByStateTop.entrySet()) {
            for (Map.Entry<String, Set<String>> topEntry : stateEntry.getValue().entrySet()) {
                Set<String> inputs = topEntry.getValue();
                if (!inputs.contains(LAMBDA) || inputs.size() < 2) {
                    continue;
                }
                String symbol = inputs.stream().filter(input -> !LAMBDA.equals(input)).findFirst().orElseThrow();
                throw new IllegalArgumentException("the automaton is not deterministic: state '" + stateEntry.getKey()
                    + "' has both a λ-transition and a transition on input '" + symbol + "' for stack top '"
                    + topEntry.getKey() + "'");
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
