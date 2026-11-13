package io.allitov.plt.automaton;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.ToString;
import lombok.Value;

@Value
@ToString(onlyExplicitlyIncluded = true)
public class FiniteAutomaton {

    public static final String EPSILON = "\u03B5";

    @ToString.Include
    Set<String> states;

    @ToString.Include
    Set<String> alphabet;

    @ToString.Include
    String start;

    @ToString.Include
    Set<String> finals;

    Set<Transition> transitions;

    Map<String, Map<String, Set<String>>> transitionsBySource;

    public FiniteAutomaton(Set<String> states,
                           Set<String> alphabet,
                           String start,
                           Set<String> finals,
                           Collection<Transition> transitions) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(alphabet, "alphabet");
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
        for (String symbol : alphabet) {
            Objects.requireNonNull(symbol, "symbol");
            if (symbol.isBlank()) {
                throw new IllegalArgumentException("alphabet symbols must not be blank");
            }
            if (EPSILON.equals(symbol)) {
                throw new IllegalArgumentException("epsilon must not be listed in the alphabet");
            }
        }
        Set<Transition> transitionCopy = new LinkedHashSet<>();
        for (Transition transition : transitions) {
            Objects.requireNonNull(transition, "transition");
            if (!stateCopy.contains(transition.from())) {
                throw new IllegalArgumentException("transition source '" + transition.from() + "' is not declared in states");
            }
            if (!stateCopy.contains(transition.to())) {
                throw new IllegalArgumentException("transition target '" + transition.to() + "' is not declared in states");
            }
            if (!alphabet.contains(transition.symbol()) && !EPSILON.equals(transition.symbol())) {
                throw new IllegalArgumentException("transition symbol '" + transition.symbol() + "' is not declared in the alphabet");
            }
            transitionCopy.add(transition);
        }
        this.states = Collections.unmodifiableSet(stateCopy);
        this.alphabet = Collections.unmodifiableSet(new LinkedHashSet<>(alphabet));
        this.start = start;
        this.finals = Collections.unmodifiableSet(new LinkedHashSet<>(finals));
        this.transitions = Collections.unmodifiableSet(transitionCopy);
        this.transitionsBySource = index(transitionCopy);
    }

    public Set<String> targets(String state, String symbol) {
        Map<String, Set<String>> bySymbol = transitionsBySource.get(state);
        if (bySymbol == null) {
            return Set.of();
        }
        return bySymbol.getOrDefault(symbol, Set.of());
    }

    public Set<String> epsilonClosure(Collection<String> source) {
        Set<String> closure = new LinkedHashSet<>(source);
        Deque<String> pending = new ArrayDeque<>(closure);
        while (!pending.isEmpty()) {
            String state = pending.pop();
            for (String target : targets(state, EPSILON)) {
                if (closure.add(target)) {
                    pending.push(target);
                }
            }
        }
        return closure;
    }

    public boolean hasEpsilonTransitions() {
        return transitions.stream().anyMatch(transition -> EPSILON.equals(transition.symbol()));
    }

    public boolean accepts(List<String> symbols) {
        Set<String> current = epsilonClosure(Set.of(start));
        for (String symbol : symbols) {
            Set<String> next = new LinkedHashSet<>();
            for (String state : current) {
                next.addAll(targets(state, symbol));
            }
            current = next.isEmpty() ? Set.of() : epsilonClosure(next);
        }
        return !Collections.disjoint(current, finals);
    }

    public boolean acceptsWord(String word) {
        List<String> symbols = new ArrayList<>(word.length());
        for (int index = 0; index < word.length(); index++) {
            symbols.add(String.valueOf(word.charAt(index)));
        }
        return accepts(symbols);
    }

    private static Map<String, Map<String, Set<String>>> index(Set<Transition> transitions) {
        Map<String, Map<String, Set<String>>> index = new LinkedHashMap<>();
        for (Transition transition : transitions) {
            index.computeIfAbsent(transition.from(), key -> new LinkedHashMap<>())
                 .computeIfAbsent(transition.symbol(), key -> new LinkedHashSet<>())
                 .add(transition.to());
        }
        Map<String, Map<String, Set<String>>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : index.entrySet()) {
            Map<String, Set<String>> bySymbol = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> symbolEntry : entry.getValue().entrySet()) {
                bySymbol.put(symbolEntry.getKey(), Collections.unmodifiableSet(symbolEntry.getValue()));
            }
            immutable.put(entry.getKey(), Collections.unmodifiableMap(bySymbol));
        }
        return Collections.unmodifiableMap(immutable);
    }
}
