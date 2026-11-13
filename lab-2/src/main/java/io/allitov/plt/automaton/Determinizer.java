package io.allitov.plt.automaton;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Determinizer {

    public FiniteAutomaton determinize(FiniteAutomaton automaton) {
        List<String> alphabet = automaton.getAlphabet().stream().sorted().toList();
        Set<Transition> transitions = new LinkedHashSet<>();
        Map<Set<String>, String> nameBySubset = new LinkedHashMap<>();
        Deque<Set<String>> pending = new ArrayDeque<>();
        Set<String> startSubset = automaton.epsilonClosure(Set.of(automaton.getStart()));
        nameBySubset.put(startSubset, subsetName(startSubset));
        pending.push(startSubset);
        while (!pending.isEmpty()) {
            Set<String> subset = pending.pop();
            for (String symbol : alphabet) {
                Set<String> merged = new LinkedHashSet<>();
                for (String state : subset) {
                    merged.addAll(automaton.targets(state, symbol));
                }
                if (merged.isEmpty()) {
                    continue;
                }
                Set<String> target = automaton.epsilonClosure(merged);
                String targetName = nameBySubset.get(target);
                if (targetName == null) {
                    targetName = subsetName(target);
                    nameBySubset.put(target, targetName);
                    pending.push(target);
                }
                transitions.add(new Transition(nameBySubset.get(subset), symbol, targetName));
            }
        }
        Set<String> states = new LinkedHashSet<>(nameBySubset.values());
        Set<String> finalStates = new LinkedHashSet<>();
        for (Map.Entry<Set<String>, String> entry : nameBySubset.entrySet()) {
            for (String state : entry.getKey()) {
                if (automaton.getFinals().contains(state)) {
                    finalStates.add(entry.getValue());
                    break;
                }
            }
        }
        return new FiniteAutomaton(states, automaton.getAlphabet(), nameBySubset.get(startSubset), finalStates, transitions);
    }

    static String subsetName(Collection<String> subset) {
        List<String> sorted = subset.stream().sorted().toList();
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        return "{" + String.join(",", sorted) + "}";
    }
}
