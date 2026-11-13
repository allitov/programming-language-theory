package io.allitov.plt.automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-side helper: brute-forces language equivalence of two automata by checking
 * every word over the alphabet up to the given length.
 */
final class Languages {

    private Languages() {
    }

    static List<List<String>> allWords(Collection<String> alphabet, int maxLength) {
        List<String> symbols = alphabet.stream().sorted().toList();
        List<List<String>> words = new ArrayList<>();
        words.add(List.of());
        List<List<String>> frontier = new ArrayList<>(words);
        for (int length = 1; length <= maxLength; length++) {
            List<List<String>> next = new ArrayList<>();
            for (List<String> word : frontier) {
                for (String symbol : symbols) {
                    List<String> extended = new ArrayList<>(word);
                    extended.add(symbol);
                    next.add(extended);
                }
            }
            words.addAll(next);
            frontier = next;
        }
        return words;
    }

    static void assertEquivalent(FiniteAutomaton expected, FiniteAutomaton actual, int maxLength) {
        for (List<String> word : allWords(expected.getAlphabet(), maxLength)) {
            assertThat(actual.accepts(word))
                .as("acceptance mismatch on word %s", word)
                .isEqualTo(expected.accepts(word));
        }
    }
}
