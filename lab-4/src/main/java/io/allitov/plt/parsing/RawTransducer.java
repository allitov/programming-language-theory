package io.allitov.plt.parsing;

import java.util.List;

record RawTransducer(
        List<String> states,
        List<String> inputAlphabet,
        List<String> stackAlphabet,
        List<String> outputAlphabet,
        String startState,
        String startStackSymbol,
        List<String> finalStates,
        List<RawTransition> transitions
) {
}
