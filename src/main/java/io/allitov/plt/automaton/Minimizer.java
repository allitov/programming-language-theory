package io.allitov.plt.automaton;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimizes a DFA by refining the partition of states into equivalence classes
 * (the Moore-style partition refinement).
 *
 * <p>Two states are equivalent when no input word distinguishes them: starting from
 * the states, both words are either accepted or rejected. The algorithm first removes
 * unreachable states, then iteratively splits classes while states inside a class
 * disagree on their own acceptance or on the class of a successor. Missing transitions
 * participate as a dedicated "no transition" marker, so a partial DFA is minimized
 * correctly and stays partial instead of receiving an artificial trap state.</p>
 */
public final class Minimizer {

    private static final int NO_TARGET = -1;

    private final DeterminismChecker checker = new DeterminismChecker();

    public FiniteAutomaton minimize(FiniteAutomaton automaton) {
        DeterminismReport report = checker.check(automaton);
        if (!report.deterministic()) {
            throw new IllegalArgumentException(
                "minimization expects a deterministic automaton, but: " + String.join("; ", report.violations()));
        }
        List<String> reachable = reachableStates(automaton);
        List<String> alphabet = automaton.getAlphabet().stream().sorted().toList();
        Map<String, Integer> classes = new LinkedHashMap<>();
        for (String state : reachable) {
            classes.put(state, automaton.getFinals().contains(state) ? 1 : 0);
        }
        while (true) {
            Map<List<Integer>, Integer> classBySignature = new LinkedHashMap<>();
            Map<String, Integer> refined = new LinkedHashMap<>();
            for (String state : reachable) {
                List<Integer> signature = new ArrayList<>();
                signature.add(classes.get(state));
                for (String symbol : alphabet) {
                    Set<String> targets = automaton.targets(state, symbol);
                    signature.add(targets.isEmpty() ? NO_TARGET : classes.get(targets.iterator().next()));
                }
                refined.put(state, classBySignature.computeIfAbsent(signature, key -> classBySignature.size()));
            }
            boolean stable = classBySignature.size() == distinctClasses(classes);
            classes = refined;
            if (stable) {
                break;
            }
        }
        Map<Integer, List<String>> statesByClass = new LinkedHashMap<>();
        for (String state : reachable) {
            statesByClass.computeIfAbsent(classes.get(state), key -> new ArrayList<>()).add(state);
        }
        Map<String, String> nameByState = new LinkedHashMap<>();
        for (List<String> classStates : statesByClass.values()) {
            String className = Determinizer.subsetName(classStates);
            for (String state : classStates) {
                nameByState.put(state, className);
            }
        }
        Set<String> states = new LinkedHashSet<>(nameByState.values());
        Set<String> finalStates = new LinkedHashSet<>();
        Set<Transition> transitions = new LinkedHashSet<>();
        for (String state : reachable) {
            if (automaton.getFinals().contains(state)) {
                finalStates.add(nameByState.get(state));
            }
            for (String symbol : alphabet) {
                for (String target : automaton.targets(state, symbol)) {
                    transitions.add(new Transition(nameByState.get(state), symbol, nameByState.get(target)));
                }
            }
        }
        return new FiniteAutomaton(states, automaton.getAlphabet(), nameByState.get(automaton.getStart()), finalStates, transitions);
    }

    private static int distinctClasses(Map<String, Integer> partition) {
        return new HashSet<>(partition.values()).size();
    }

    private static List<String> reachableStates(FiniteAutomaton automaton) {
        List<String> alphabet = automaton.getAlphabet().stream().sorted().toList();
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        visited.add(automaton.getStart());
        pending.push(automaton.getStart());
        while (!pending.isEmpty()) {
            String state = pending.pop();
            for (String symbol : alphabet) {
                for (String target : automaton.targets(state, symbol)) {
                    if (visited.add(target)) {
                        pending.push(target);
                    }
                }
            }
        }
        return List.copyOf(visited);
    }
}
