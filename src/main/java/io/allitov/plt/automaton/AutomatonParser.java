package io.allitov.plt.automaton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import io.allitov.plt.exception.AutomatonFormatException;

/**
 * Parses the line-oriented text description of a finite automaton:
 * <pre>
 * # comment line
 * states: q0 q1 q2
 * alphabet: a b
 * start: q0
 * final: q2
 *
 * transitions:
 * q0 a q0 q1
 * q1 eps q2
 * </pre>
 * The {@code eps} (or the literal epsilon letter) symbol denotes an epsilon transition.
 * A transition line maps one source state and one symbol to one or more target states,
 * which is exactly where non-determinism is expressed. All errors are reported with
 * the line number of the offending text.
 */
public final class AutomatonParser {

    private static final String EPSILON_ALIAS = "eps";

    private AutomatonParser() {
    }

    public static FiniteAutomaton parse(String text) {
        Objects.requireNonNull(text, "text");
        Set<String> states = new LinkedHashSet<>();
        Set<String> alphabet = new LinkedHashSet<>();
        Set<String> finalStates = new LinkedHashSet<>();
        Set<String> declaredKeys = new HashSet<>();
        List<RawTransition> rawTransitions = new ArrayList<>();
        String start = null;
        boolean insideTransitions = false;
        int lineNumber = 0;
        for (String line : text.split("\\R")) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                continue;
            }
            int separator = trimmed.indexOf(':');
            if (separator > 0 && trimmed.substring(0, separator).matches("[A-Za-z]+")) {
                String key = trimmed.substring(0, separator).toLowerCase(Locale.ROOT);
                List<String> tokens = tokens(trimmed.substring(separator + 1));
                switch (key) {
                    case "states" -> {
                        requireNewSection(declaredKeys, key, lineNumber);
                        states.addAll(tokens);
                    }
                    case "alphabet" -> {
                        requireNewSection(declaredKeys, key, lineNumber);
                        alphabet.addAll(tokens);
                    }
                    case "start" -> {
                        requireNewSection(declaredKeys, key, lineNumber);
                        if (tokens.size() != 1) {
                            throw error(lineNumber, "'start:' expects exactly one state name");
                        }
                        start = tokens.get(0);
                    }
                    case "final", "finals" -> {
                        requireNewSection(declaredKeys, "final", lineNumber);
                        finalStates.addAll(tokens);
                    }
                    case "transitions" -> {
                        requireNewSection(declaredKeys, key, lineNumber);
                        if (!tokens.isEmpty()) {
                            throw error(lineNumber, "'transitions:' does not take a value");
                        }
                        insideTransitions = true;
                    }
                    default -> throw error(lineNumber, "unknown key '" + key + "'");
                }
            } else if (insideTransitions) {
                List<String> tokens = List.of(trimmed.split("\\s+"));
                if (tokens.size() < 3) {
                    throw error(lineNumber, "a transition must be 'state symbol target [target ...]'");
                }
                rawTransitions.add(new RawTransition(lineNumber, tokens.get(0), normalizeSymbol(tokens.get(1)),
                    List.copyOf(tokens.subList(2, tokens.size()))));
            } else {
                throw error(lineNumber, "unexpected content outside of any section (missing 'transitions:'?)");
            }
        }
        if (start == null) {
            throw new AutomatonFormatException("missing 'start:' declaration");
        }
        if (!states.contains(start)) {
            throw new AutomatonFormatException("start state '" + start + "' is not declared in 'states:'");
        }
        for (String finalState : finalStates) {
            if (!states.contains(finalState)) {
                throw new AutomatonFormatException("final state '" + finalState + "' is not declared in 'states:'");
            }
        }
        Set<Transition> transitions = new LinkedHashSet<>();
        for (RawTransition raw : rawTransitions) {
            if (!states.contains(raw.from())) {
                throw error(raw.lineNumber(), "unknown state '" + raw.from() + "'");
            }
            if (!FiniteAutomaton.EPSILON.equals(raw.symbol()) && !alphabet.contains(raw.symbol())) {
                throw error(raw.lineNumber(), "unknown symbol '" + displaySymbol(raw.symbol()) + "'");
            }
            for (String target : raw.targets()) {
                if (!states.contains(target)) {
                    throw error(raw.lineNumber(), "unknown state '" + target + "'");
                }
                transitions.add(new Transition(raw.from(), raw.symbol(), target));
            }
        }
        return new FiniteAutomaton(states, alphabet, start, finalStates, transitions);
    }

    /** Renders the epsilon symbol back to its ASCII alias for messages and printing. */
    static String displaySymbol(String symbol) {
        return FiniteAutomaton.EPSILON.equals(symbol) ? EPSILON_ALIAS : symbol;
    }

    private static List<String> tokens(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? List.of() : List.of(trimmed.split("\\s+"));
    }

    private static void requireNewSection(Set<String> declaredKeys, String key, int lineNumber) {
        if (!declaredKeys.add(key)) {
            throw error(lineNumber, "duplicate '" + key + ":' section");
        }
    }

    private static String normalizeSymbol(String symbol) {
        return EPSILON_ALIAS.equals(symbol) || FiniteAutomaton.EPSILON.equals(symbol)
            ? FiniteAutomaton.EPSILON
            : symbol;
    }

    private static AutomatonFormatException error(int lineNumber, String message) {
        return new AutomatonFormatException("line " + lineNumber + ": " + message);
    }

    private record RawTransition(int lineNumber, String from, String symbol, List<String> targets) {
    }
}
