package io.allitov.plt.pda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import io.allitov.plt.exception.AutomatonFormatException;
import lombok.experimental.UtilityClass;

/**
 * Parses the textual description of a deterministic pushdown automaton:
 *
 * <pre>
 * states: q1 q2 q3 q4 q5 q6
 * alphabet: 0 1
 * stack: Z 0 1
 * start: q4
 * final: q1 q5
 *
 * transitions:
 * q4 0 Z = q5 0Z
 * q5 1 0 = q5 λ
 * </pre>
 *
 * <p>The '=' separator in a transition line is optional. The push part (the replacement of the stack
 * top) may be λ (the symbol is removed), the top symbol itself (nothing changes) or any sequence
 * of stack symbols whose first symbol becomes the new stack top. A λ (or ε, or "eps") input means
 * a move that consumes no input.</p>
 */
@UtilityClass
public class PdaParser {

    private final String EPSILON_ALIAS = "eps";

    public PushdownAutomaton parse(String text) {
        Objects.requireNonNull(text, "text");
        Set<String> states = new LinkedHashSet<>();
        Set<String> alphabet = new LinkedHashSet<>();
        Set<String> stackAlphabet = new LinkedHashSet<>();
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
                    case "stack" -> {
                        requireNewSection(declaredKeys, key, lineNumber);
                        stackAlphabet.addAll(tokens);
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
                List<String> tokens = new ArrayList<>(List.of(trimmed.split("\\s+")));
                if (tokens.stream().filter("="::equals).count() > 1) {
                    throw error(lineNumber, "a transition must contain at most one '=' separator");
                }
                tokens.removeIf("="::equals);
                if (tokens.size() < 5) {
                    throw error(lineNumber, "a transition must be 'state input stackTop = state push'");
                }
                rawTransitions.add(new RawTransition(lineNumber, tokens.get(0), normalizeInput(tokens.get(1)),
                    tokens.get(2), tokens.get(3), String.join("", tokens.subList(4, tokens.size()))));
            } else {
                throw error(lineNumber, "unexpected content outside of any section (missing 'transitions:'?)");
            }
        }
        requireSection(declaredKeys, "states");
        requireSection(declaredKeys, "alphabet");
        requireSection(declaredKeys, "stack");
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
        List<PdaTransition> transitions = new ArrayList<>();
        for (RawTransition raw : rawTransitions) {
            if (!states.contains(raw.from())) {
                throw error(raw.lineNumber(), "unknown state '" + raw.from() + "'");
            }
            if (!states.contains(raw.to())) {
                throw error(raw.lineNumber(), "unknown state '" + raw.to() + "'");
            }
            if (!PushdownAutomaton.LAMBDA.equals(raw.input()) && !alphabet.contains(raw.input())) {
                throw error(raw.lineNumber(), "unknown input symbol '" + raw.input() + "'");
            }
            if (!stackAlphabet.contains(raw.top())) {
                throw error(raw.lineNumber(), "unknown stack symbol '" + raw.top() + "'");
            }
            transitions.add(new PdaTransition(raw.from(), raw.input(), raw.top(), raw.to(),
                splitPush(raw.lineNumber(), raw.push(), stackAlphabet)));
        }
        return new PushdownAutomaton(states, alphabet, stackAlphabet, start, finalStates, transitions);
    }

    private List<String> splitPush(int lineNumber, String push, Set<String> stackAlphabet) {
        if (isLambda(push)) {
            return List.of();
        }
        List<String> ordered = stackAlphabet.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
        List<String> symbols = new ArrayList<>();
        int position = 0;
        while (position < push.length()) {
            String match = null;
            for (String candidate : ordered) {
                if (push.startsWith(candidate, position)) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) {
                throw error(lineNumber, "cannot split the stack replacement '" + push + "' into stack symbols (unknown symbol near position "
                    + (position + 1) + ")");
            }
            symbols.add(match);
            position += match.length();
        }
        return List.copyOf(symbols);
    }

    private boolean isLambda(String token) {
        return PushdownAutomaton.LAMBDA.equals(token) || PushdownAutomaton.EPSILON.equals(token) || EPSILON_ALIAS.equals(token);
    }

    private String normalizeInput(String token) {
        return isLambda(token) ? PushdownAutomaton.LAMBDA : token;
    }

    private List<String> tokens(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? List.of() : List.of(trimmed.split("\\s+"));
    }

    private void requireNewSection(Set<String> declaredKeys, String key, int lineNumber) {
        if (!declaredKeys.add(key)) {
            throw error(lineNumber, "duplicate '" + key + ":' section");
        }
    }

    private void requireSection(Set<String> declaredKeys, String key) {
        if (!declaredKeys.contains(key)) {
            throw new AutomatonFormatException("missing '" + key + ":' declaration");
        }
    }

    private AutomatonFormatException error(int lineNumber, String message) {
        return new AutomatonFormatException("line " + lineNumber + ": " + message);
    }

    private record RawTransition(int lineNumber, String from, String input, String top, String to, String push) {
    }
}
