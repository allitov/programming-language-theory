package io.allitov.plt.parsing;

record RawTransition(
        String state,
        String input,
        String stackTop,
        String newState,
        String push,
        String output
) {
}
