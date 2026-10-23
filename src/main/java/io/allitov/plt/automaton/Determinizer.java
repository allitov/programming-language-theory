package io.allitov.plt.automaton;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts an arbitrary finite automaton into an equivalent DFA using the subset
 * construction (Rabin-Scott powerset construction).
 *
 * <p>Every DFA state represents a set of NFA states that can be active simultaneously;
 * before and after each step epsilon closures are taken so that epsilon transitions
 * disappear. A subset is accepting when it contains at least one accepting NFA state.
 * A DFA state that is the only element of its subset keeps the original state name,
 * otherwise it is named "{q0,q1,q3}".</p>
 */
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

    /** Names a subset of states: a singleton keeps its own name, larger sets become "{q0,q1,q2}". */
    static String subsetName(Collection<String> subset) {
        List<String> sorted = subset.stream().sorted().toList();
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        return "{" + String.join(",", sorted) + "}";
    }
}
