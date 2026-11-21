package io.allitov.plt.transducer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import io.allitov.plt.exception.ConfigurationException;
import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads a pushdown transducer from its json configuration (Jackson) and maps it into the
 * {@link Transducer} model with full validation: required fields, symbols inside the declared
 * alphabets, determinism (unique {@code (q, a, z)} rules and no λ-rule conflicting with a real-symbol
 * rule for the same pair {@code (q, z)}).
 */
@UtilityClass
public class TransducerParser {

    private final JsonMapper MAPPER = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    public Transducer parse(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (NoSuchFileException _) {
            throw new IOException("the transducer file '" + file + "' does not exist");
        } catch (InvalidPathException _) {
            throw new IOException("invalid transducer file path '" + file + "'");
        }
    }

    public Transducer parse(String json) {
        Objects.requireNonNull(json, "json");
        RawTransducer raw;
        try {
            raw = MAPPER.readValue(json, RawTransducer.class);
        } catch (JacksonException exception) {
            throw new ConfigurationException("cannot parse the transducer json: " + exception.getMessage(), exception);
        }
        return build(raw);
    }

    private Transducer build(RawTransducer raw) {
        if (raw == null) {
            throw new ConfigurationException("the transducer json is empty");
        }
        requireField(raw.states(), "states");
        requireField(raw.inputAlphabet(), "inputAlphabet");
        requireField(raw.stackAlphabet(), "stackAlphabet");
        requireField(raw.outputAlphabet(), "outputAlphabet");
        requireField(raw.startState(), "startState");
        requireField(raw.startStackSymbol(), "startStackSymbol");
        requireField(raw.finalStates(), "finalStates");
        requireField(raw.transitions(), "transitions");
        List<Transition> transitions = new ArrayList<>();
        int number = 0;
        for (RawTransition rawTransition : raw.transitions()) {
            number++;
            requireField(rawTransition.state(), "state of transition #" + number);
            requireField(rawTransition.input(), "input of transition #" + number);
            requireField(rawTransition.stackTop(), "stackTop of transition #" + number);
            requireField(rawTransition.newState(), "newState of transition #" + number);
            requireField(rawTransition.push(), "push of transition #" + number);
            requireField(rawTransition.output(), "output of transition #" + number);
            List<String> push = splitChain(rawTransition.push(), raw.stackAlphabet(), "push", number);
            List<String> output = splitChain(rawTransition.output(), raw.outputAlphabet(), "output", number);
            transitions.add(new Transition(rawTransition.state(), rawTransition.input(), rawTransition.stackTop(),
                rawTransition.newState(), push, output));
        }
        try {
            return new Transducer(new LinkedHashSet<>(raw.states()), new LinkedHashSet<>(raw.inputAlphabet()),
                new LinkedHashSet<>(raw.stackAlphabet()), new LinkedHashSet<>(raw.outputAlphabet()),
                raw.startState(), raw.startStackSymbol(), new LinkedHashSet<>(raw.finalStates()), transitions);
        } catch (IllegalArgumentException exception) {
            throw new ConfigurationException(exception.getMessage(), exception);
        }
    }

    /**
     * Splits a rule chain into atomic symbols by matching the longest declared symbol first
     * (needed because symbols such as {@code (0} or {@code num} span several characters).
     * λ and ε denote the empty chain.
     */
    private List<String> splitChain(String chain, List<String> alphabet, String field, int number) {
        if (Transducer.LAMBDA.equals(chain) || Transducer.EPSILON.equals(chain)) {
            return List.of();
        }
        List<String> ordered = alphabet.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
        List<String> symbols = new ArrayList<>();
        int position = 0;
        while (position < chain.length()) {
            String match = null;
            for (String candidate : ordered) {
                if (chain.startsWith(candidate, position)) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) {
                throw new ConfigurationException("transition #" + number + ": the " + field + " chain '" + chain
                    + "' contains an unknown symbol near position " + (position + 1)
                    + " (not declared in the corresponding alphabet)");
            }
            symbols.add(match);
            position += match.length();
        }
        return List.copyOf(symbols);
    }

    private void requireField(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("required field '" + name + "' is missing");
        }
    }

    private void requireField(List<?> value, String name) {
        if (value == null || value.isEmpty()) {
            throw new ConfigurationException("required field '" + name + "' is missing or empty");
        }
    }

    private record RawTransducer(List<String> states, List<String> inputAlphabet, List<String> stackAlphabet,
                                 List<String> outputAlphabet, String startState, String startStackSymbol,
                                 List<String> finalStates, List<RawTransition> transitions) {
    }

    private record RawTransition(String state, String input, String stackTop, String newState,
                                 String push, String output) {
    }
}
