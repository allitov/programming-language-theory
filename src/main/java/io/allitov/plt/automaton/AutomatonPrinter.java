package io.allitov.plt.automaton;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Renders automata for humans: back into the input text format (so the result of
 * a run can be fed into the program again) and into the Graphviz DOT format.
 */
public final class AutomatonPrinter {

    private AutomatonPrinter() {
    }

    /** Prints the automaton in the same line-oriented format the parser accepts. */
    public static String print(FiniteAutomaton automaton) {
        StringBuilder out = new StringBuilder();
        out.append("states: ").append(String.join(" ", sorted(automaton.getStates()))).append('\n');
        out.append("alphabet: ").append(String.join(" ", sorted(automaton.getAlphabet()))).append('\n');
        out.append("start: ").append(automaton.getStart()).append('\n');
        out.append("final: ").append(String.join(" ", sorted(automaton.getFinals()))).append('\n');
        out.append("transitions:\n");
        Map<String, Map<String, Set<String>>> grouped = new TreeMap<>();
        for (Transition transition : automaton.getTransitions()) {
            grouped.computeIfAbsent(transition.from(), key -> new TreeMap<>())
                   .computeIfAbsent(AutomatonParser.displaySymbol(transition.symbol()), key -> new TreeSet<>())
                   .add(transition.to());
        }
        for (Map.Entry<String, Map<String, Set<String>>> source : grouped.entrySet()) {
            for (Map.Entry<String, Set<String>> step : source.getValue().entrySet()) {
                out.append(source.getKey()).append(' ').append(step.getKey()).append(' ')
                   .append(String.join(" ", step.getValue())).append('\n');
            }
        }
        return out.toString();
    }

    /** Exports the automaton as a Graphviz DOT digraph. */
    public static String toDot(FiniteAutomaton automaton) {
        StringBuilder out = new StringBuilder();
        out.append("digraph automaton {\n");
        out.append("    rankdir=LR;\n");
        out.append("    node [shape = circle];\n");
        out.append("    __start [shape = point];\n");
        out.append("    __start -> ").append(quoted(automaton.getStart())).append(";\n");
        for (String state : sorted(automaton.getStates())) {
            if (automaton.getFinals().contains(state)) {
                out.append("    ").append(quoted(state)).append(" [shape = doublecircle];\n");
            }
        }
        List<Transition> sortedTransitions = automaton.getTransitions().stream()
            .sorted(Comparator.comparing(Transition::from)
                .thenComparing(Transition::symbol)
                .thenComparing(Transition::to))
            .toList();
        for (Transition transition : sortedTransitions) {
            out.append("    ").append(quoted(transition.from())).append(" -> ").append(quoted(transition.to()))
               .append(" [label = \"").append(AutomatonParser.displaySymbol(transition.symbol())).append("\"];\n");
        }
        out.append("}\n");
        return out.toString();
    }

    private static List<String> sorted(Set<String> states) {
        return states.stream().sorted().toList();
    }

    private static String quoted(String state) {
        return "\"" + escape(state) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
