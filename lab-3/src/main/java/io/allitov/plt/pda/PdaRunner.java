package io.allitov.plt.pda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class PdaRunner {

    public static final int STEP_LIMIT = 10_000;

    public RunResult run(PushdownAutomaton automaton, String chain) {
        Objects.requireNonNull(automaton, "automaton");
        Objects.requireNonNull(chain, "chain");
        if (isEmptyChain(chain)) {
            return run(automaton, List.of());
        }
        try {
            return run(automaton, tokenize(chain, automaton.getAlphabet()));
        } catch (ChainFormatException exception) {
            Configuration initial = new Configuration(automaton.getStart(), charactersOf(chain),
                List.of(PushdownAutomaton.INITIAL_STACK));
            return RunResult.rejected(initial, initial, List.of(),
                "the string contains the symbol '" + exception.getSymbol() + "' (position " + (exception.getPosition() + 1)
                    + "), which is not in the automaton alphabet " + automaton.getAlphabet());
        }
    }

    public RunResult run(PushdownAutomaton automaton, List<String> input) {
        Objects.requireNonNull(automaton, "automaton");
        Objects.requireNonNull(input, "input");
        List<String> symbols = List.copyOf(input);
        Configuration initial = new Configuration(automaton.getStart(), symbols, List.of(PushdownAutomaton.INITIAL_STACK));
        Configuration current = initial;
        List<Step> steps = new ArrayList<>();
        while (true) {
            if (steps.size() >= STEP_LIMIT) {
                return RunResult.rejected(initial, current, steps, "the step limit of " + STEP_LIMIT
                    + " has been exceeded; the automaton probably loops on λ-transitions");
            }
            if (current.input().isEmpty()) {
                if (automaton.getFinals().contains(current.state())) {
                    return RunResult.accepted(initial, current, steps);
                }
                Optional<PdaTransition> lambdaMove = lookup(automaton, current, PushdownAutomaton.LAMBDA);
                if (lambdaMove.isEmpty()) {
                    return RunResult.rejected(initial, current, steps, "the input has been fully consumed but the automaton stopped in state '"
                        + current.state() + "', which is not a final state (final states: " + automaton.getFinals() + ")");
                }
                current = apply(current, lambdaMove.get(), false, steps);
                continue;
            }
            String symbol = current.input().getFirst();
            Optional<PdaTransition> move = lookup(automaton, current, symbol);
            boolean consumes = true;
            if (move.isEmpty()) {
                move = lookup(automaton, current, PushdownAutomaton.LAMBDA);
                consumes = false;
            }
            if (move.isEmpty()) {
                return RunResult.rejected(initial, current, steps, "no transition is defined for the configuration "
                    + PdaPrinter.format(current) + ": the automaton got stuck while reading symbol '" + symbol
                    + "' (position " + (symbols.size() - current.input().size() + 1) + ")");
            }
            current = apply(current, move.get(), consumes, steps);
        }
    }

    private static Configuration apply(Configuration current, PdaTransition transition, boolean consumesInput, List<Step> steps) {
        List<String> remainingInput = consumesInput
            ? current.input().subList(1, current.input().size())
            : current.input();
        List<String> remainingStack = current.stack().subList(1, current.stack().size());
        List<String> stack = new ArrayList<>(transition.push());
        stack.addAll(remainingStack);
        Configuration next = new Configuration(transition.to(), List.copyOf(remainingInput), List.copyOf(stack));
        steps.add(new Step(steps.size() + 1, current, next, transition));
        return next;
    }

    private static Optional<PdaTransition> lookup(PushdownAutomaton automaton, Configuration current, String input) {
        if (current.stack().isEmpty()) {
            return Optional.empty();
        }
        return automaton.transition(current.state(), input, current.stack().getFirst());
    }

    static List<String> tokenize(String chain, Set<String> alphabet) {
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
                throw new ChainFormatException(position, chain.charAt(position));
            }
            symbols.add(match);
            position += match.length();
        }
        return List.copyOf(symbols);
    }

    private static boolean isEmptyChain(String chain) {
        return chain.isEmpty() || PushdownAutomaton.LAMBDA.equals(chain) || PushdownAutomaton.EPSILON.equals(chain);
    }

    private static List<String> charactersOf(String chain) {
        return chain.chars().mapToObj(character -> String.valueOf((char) character)).toList();
    }

    private static final class ChainFormatException extends RuntimeException {

        private final int position;
        private final char symbol;

        private ChainFormatException(int position, char symbol) {
            super("unknown symbol '" + symbol + "' at position " + (position + 1));
            this.position = position;
            this.symbol = symbol;
        }

        int getPosition() {
            return position;
        }

        char getSymbol() {
            return symbol;
        }
    }
}
