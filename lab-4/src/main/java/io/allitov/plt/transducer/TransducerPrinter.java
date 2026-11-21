package io.allitov.plt.transducer;

import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * Renders transducer objects in the conventions of the task: a configuration is the quadruple
 * {@code (q, x, α, y)} with the stack written top-first; the empty chain (input, stack, output tape)
 * is denoted λ.
 */
@UtilityClass
public class TransducerPrinter {

    public String format(Configuration configuration) {
        return "(" + configuration.state() + ", " + formatInput(configuration.input()) + ", "
            + formatStack(configuration.stack()) + ", " + formatTape(configuration.output()) + ")";
    }

    public String formatStep(Step step) {
        return step.number() + ") " + format(step.before()) + " -> " + format(step.after());
    }

    public String formatTransition(Transition transition) {
        return "δ(" + transition.state() + ", " + transition.input() + ", " + transition.stackTop() + ") = ("
            + transition.newState() + ", " + joinChain(transition.push()) + ", " + joinChain(transition.output()) + ")";
    }

    public String summary(Transducer transducer) {
        StringBuilder summary = new StringBuilder("Pushdown transducer P = (Q, V, Z, V', δ, q0, z0, F) loaded:");
        summary.append("\n  States (Q): ").append(String.join(" ", transducer.getStates()));
        summary.append("\n  Input alphabet (V): ").append(joinQuoted(transducer.getInputAlphabet()));
        summary.append("\n  Stack alphabet (Z): ").append(joinQuoted(transducer.getStackAlphabet()));
        summary.append("\n  Output alphabet (V'): ").append(joinQuoted(transducer.getOutputAlphabet()));
        summary.append("\n  Start state (q0): ").append(transducer.getStartState());
        summary.append("\n  Start stack symbol (z0): ").append(transducer.getStartStackSymbol());
        summary.append("\n  Final states (F): ").append(String.join(" ", transducer.getFinalStates()));
        summary.append("\n  Transitions (|δ|): ").append(transducer.getTransitions().size());
        for (Transition transition : transducer.getTransitions()) {
            summary.append("\n    ").append(formatTransition(transition));
        }
        return summary.toString();
    }

    private String formatInput(List<Lexeme> input) {
        if (input.isEmpty()) {
            return Transducer.LAMBDA;
        }
        return String.join(" ", input.stream().map(Lexeme::text).toList());
    }

    private String formatStack(List<String> stack) {
        return stack.isEmpty() ? Transducer.LAMBDA : String.join("", stack);
    }

    private String formatTape(String output) {
        return output.isEmpty() ? Transducer.LAMBDA : output;
    }

    private String joinChain(List<String> symbols) {
        return symbols.isEmpty() ? Transducer.LAMBDA : String.join("", symbols);
    }

    private String joinQuoted(java.util.Set<String> symbols) {
        return String.join(", ", symbols.stream().map(symbol -> "'" + symbol + "'").toList());
    }
}
