package io.allitov.plt.transducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.allitov.plt.exception.TranslationException;

/**
 * The universal tick interpreter of a pushdown transducer: by the current configuration
 * {@code (q, x, α, y)} and a rule from δ it performs the step {@code (q, ax, zγ, y) ⊢ (r, x, αγ, yt)}.
 * Supports transitions by real input symbols as well as λ-transitions (performed without reading a
 * symbol, e.g. the final pops of the stack when the input is exhausted). The transducer is treated
 * as deterministic; whenever no rule fits the current configuration the input is rejected with a
 * {@link TranslationException} carrying the position of the offending lexeme.
 */
public class TransducerRunner {

    public static final int STEP_LIMIT = 10_000;

    public TranslationResult run(Transducer transducer, List<Lexeme> input) {
        Objects.requireNonNull(transducer, "transducer");
        Objects.requireNonNull(input, "input");
        if (input.isEmpty()) {
            throw new TranslationException("the input chain is empty", 0);
        }
        Configuration initial = new Configuration(transducer.getStartState(), input,
            List.of(transducer.getStartStackSymbol()), "");
        Configuration current = initial;
        List<Step> steps = new ArrayList<>();
        int endPosition = input.getLast().position();
        while (true) {
            if (steps.size() >= STEP_LIMIT) {
                throw new TranslationException("the step limit of " + STEP_LIMIT + " has been exceeded; "
                    + "the transducer probably loops on λ-transitions", endPosition);
            }
            if (current.input().isEmpty()) {
                Optional<Transition> lambdaMove = lookup(transducer, current, Transducer.LAMBDA);
                if (lambdaMove.isEmpty()) {
                    if (transducer.getFinalStates().contains(current.state())) {
                        return new TranslationResult(initial, current, steps);
                    }
                    throw new TranslationException("the input has been fully consumed, but the transducer stopped in "
                        + "the state '" + current.state() + "', which is not a final state " + transducer.getFinalStates(),
                        endPosition);
                }
                current = apply(current, lambdaMove.get(), false, steps);
                continue;
            }
            Lexeme lexeme = current.input().getFirst();
            Optional<Transition> move = lookup(transducer, current, lexeme.symbol());
            boolean consumed = true;
            if (move.isEmpty()) {
                move = lookup(transducer, current, Transducer.LAMBDA);
                consumed = false;
            }
            if (move.isEmpty()) {
                String stackTop = current.stack().isEmpty() ? Transducer.LAMBDA : current.stack().getFirst();
                throw new TranslationException("no transition is defined for the configuration (state '"
                    + current.state() + "', input '" + lexeme.symbol() + "', stack top '" + stackTop
                    + "') while reading the lexeme '" + lexeme.text() + "' (position " + (lexeme.position() + 1)
                    + "); the expression violates the syntax of arithmetic expressions", lexeme.position());
            }
            current = apply(current, move.get(), consumed, steps);
        }
    }

    private static Configuration apply(Configuration current, Transition transition, boolean consumesInput,
                                       List<Step> steps) {
        List<Lexeme> remainingInput = consumesInput
            ? current.input().subList(1, current.input().size())
            : current.input();
        List<String> remainingStack = current.stack().subList(1, current.stack().size());
        List<String> stack = new ArrayList<>(transition.push());
        stack.addAll(remainingStack);
        String lexemeText = consumesInput ? current.input().getFirst().text() : null;
        Configuration next = new Configuration(transition.newState(), remainingInput, stack,
            current.output() + emit(transition.output(), lexemeText));
        steps.add(new Step(steps.size() + 1, current, next, transition, consumesInput));
        return next;
    }

    /** Builds the emitted chain; the token {@code num} echoes the text of the consumed lexeme. */
    private static String emit(List<String> outputTokens, String lexemeText) {
        if (outputTokens.isEmpty()) {
            return "";
        }
        StringBuilder tape = new StringBuilder();
        for (String token : outputTokens) {
            tape.append(Transition.NUM_TOKEN.equals(token) && lexemeText != null ? lexemeText : token);
        }
        return tape.toString();
    }

    private static Optional<Transition> lookup(Transducer transducer, Configuration current, String input) {
        if (current.stack().isEmpty()) {
            return Optional.empty();
        }
        return transducer.transition(current.state(), input, current.stack().getFirst());
    }
}
