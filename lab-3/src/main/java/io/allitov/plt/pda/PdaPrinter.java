package io.allitov.plt.pda;

import java.util.List;
import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PdaPrinter {

    public String format(Configuration configuration) {
        return "(" + configuration.state() + ", " + join(configuration.input()) + ", " + join(configuration.stack()) + ")";
    }

    public String formatStep(Step step) {
        return step.number() + ") " + format(step.before()) + " -> " + format(step.after());
    }

    public String formatTransition(PdaTransition transition) {
        String push = transition.push().isEmpty() ? PushdownAutomaton.LAMBDA : String.join("", transition.push());
        return transition.from() + " " + transition.input() + " " + transition.top() + " = " + transition.to() + " " + push;
    }

    public String formatChain(String chain) {
        return chain.isEmpty() ? PushdownAutomaton.LAMBDA : chain;
    }

    public String summary(PushdownAutomaton automaton) {
        StringBuilder summary = new StringBuilder("Automaton loaded:");
        summary.append("\n  States: ").append(String.join(" ", automaton.getStates()));
        summary.append("\n  Input alphabet: ").append(joinSymbols(automaton.getAlphabet()));
        summary.append("\n  Stack alphabet: ").append(joinSymbols(automaton.getStackAlphabet()));
        summary.append("\n  Start state: ").append(automaton.getStart());
        summary.append("\n  Final states: ")
               .append(automaton.getFinals().isEmpty() ? "(none)" : String.join(" ", automaton.getFinals()));
        summary.append("\n  Transitions:");
        if (automaton.getTransitions().isEmpty()) {
            summary.append(" (none)");
        } else {
            for (PdaTransition transition : automaton.getTransitions()) {
                summary.append("\n    ").append(formatTransition(transition));
            }
        }
        return summary.toString();
    }

    private String join(List<String> symbols) {
        return symbols.isEmpty() ? PushdownAutomaton.LAMBDA : String.join("", symbols);
    }

    private String joinSymbols(Set<String> symbols) {
        return symbols.isEmpty() ? PushdownAutomaton.LAMBDA : String.join(" ", symbols);
    }
}
