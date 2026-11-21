package io.allitov.plt.transducer;

import io.allitov.plt.exception.TranslationException;
import io.allitov.plt.parsing.TransducerParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransducerRunner: ticks, lambda-transitions, acceptance and errors")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransducerRunnerTest {

    private final TransducerRunner runner = new TransducerRunner();
    private Transducer transducer;

    @BeforeAll
    void setUp() {
        transducer = TestSupport.rpnTransducer();
    }

    private TranslationResult translate(String line) {
        return runner.run(transducer, Lexer.tokenize(line));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("validExpressions")
    void translatesExpressionsIntoRpn(String input, String expectedRpn) {
        TranslationResult result = translate(input);
        assertThat(result.last().state()).isEqualTo("q8");
        assertThat(result.output().strip()).isEqualTo(expectedRpn);
    }

    Stream<org.junit.jupiter.params.provider.Arguments> validExpressions() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("5 + 3* 2", "5 3 2 * +"),
            org.junit.jupiter.params.provider.Arguments.of("5+3", "5 3 +"),
            org.junit.jupiter.params.provider.Arguments.of("5 - 3 + 2", "5 3 - 2 +"),
            org.junit.jupiter.params.provider.Arguments.of("8 - 3 - 2", "8 3 - 2 -"),
            org.junit.jupiter.params.provider.Arguments.of("2 * 3 / 4", "2 3 * 4 /"),
            org.junit.jupiter.params.provider.Arguments.of("2 * 3 / 4 * 5", "2 3 * 4 / 5 *"),
            org.junit.jupiter.params.provider.Arguments.of("(5 + 3) * 2", "5 3 + 2 *"),
            org.junit.jupiter.params.provider.Arguments.of("10 * (3 + 2)", "10 3 2 + *"),
            org.junit.jupiter.params.provider.Arguments.of("((1 + 2) * 3)", "1 2 + 3 *"),
            org.junit.jupiter.params.provider.Arguments.of("(1 + 2) * (3 + 4)", "1 2 + 3 4 + *"),
            org.junit.jupiter.params.provider.Arguments.of("1 + 2*3 - 4/2", "1 2 3 * + 4 2 / -"),
            org.junit.jupiter.params.provider.Arguments.of("100 / (2 * 5) / 2", "100 2 5 * / 2 /"),
            org.junit.jupiter.params.provider.Arguments.of("12 + 345", "12 345 +"),
            org.junit.jupiter.params.provider.Arguments.of("7", "7"),
            org.junit.jupiter.params.provider.Arguments.of("(7)", "7"),
            org.junit.jupiter.params.provider.Arguments.of("((2))", "2")
        );
    }

    @Test
    @DisplayName("control example 5 + 3* 2: configuration sequence matches the manual parse")
    void controlExampleMatchesManualParse() {
        TranslationResult result = translate("5 + 3* 2");
        List<String> steps = result.steps().stream().map(TransducerPrinter::formatStep).toList();
        assertThat(steps).containsExactly(
            "1) (q0, 5 + 3 * 2 ⊥, Z, λ) -> (q1, + 3 * 2 ⊥, Z, 5)",
            "2) (q1, + 3 * 2 ⊥, Z, 5) -> (q2, 3 * 2 ⊥, Z, 5)",
            "3) (q2, 3 * 2 ⊥, Z, 5) -> (q0, 3 * 2 ⊥, +Z, 5)",
            "4) (q0, 3 * 2 ⊥, +Z, 5) -> (q1, * 2 ⊥, +Z, 5 3)",
            "5) (q1, * 2 ⊥, +Z, 5 3) -> (q4, 2 ⊥, +Z, 5 3)",
            "6) (q4, 2 ⊥, +Z, 5 3) -> (q0, 2 ⊥, *+Z, 5 3)",
            "7) (q0, 2 ⊥, *+Z, 5 3) -> (q1, ⊥, *+Z, 5 3 2)",
            "8) (q1, ⊥, *+Z, 5 3 2) -> (q7, λ, *+Z, 5 3 2)",
            "9) (q7, λ, *+Z, 5 3 2) -> (q7, λ, +Z, 5 3 2 *)",
            "10) (q7, λ, +Z, 5 3 2 *) -> (q7, λ, Z, 5 3 2 * +)",
            "11) (q7, λ, Z, 5 3 2 * +) -> (q8, λ, Z, 5 3 2 * +)"
        );
        assertThat(result.output().strip()).isEqualTo("5 3 2 * +");
    }

    @Test
    @DisplayName("λ-transitions are performed without reading an input symbol")
    void performsLambdaTicksWithoutReadingInput() {
        TranslationResult result = translate("5 + 3* 2");
        List<Integer> lambdaTicks = result.steps().stream()
            .filter(step -> !step.inputConsumed())
            .map(Step::number)
            .toList();
        assertThat(lambdaTicks).containsExactly(3, 6, 9, 10, 11);
        Step finalPop = result.steps().get(9);
        assertThat(finalPop.transition().input()).isEqualTo("λ");
        assertThat(finalPop.before().output()).isEqualTo("5 3 2 *");
        assertThat(finalPop.after().output()).isEqualTo("5 3 2 * +");
    }

    @Test
    void firstLambdaTickOfOperatorProcessingPopsPendingOperation() {
        // "8 - 3 - 2": while processing the second '-' the pending '-' is popped by a λ-move
        TranslationResult result = translate("8 - 3 - 2");
        Step step = result.steps().get(5);
        assertThat(step.inputConsumed()).isFalse();
        assertThat(step.before().output()).isEqualTo("8 3");
        assertThat(step.after().output()).isEqualTo("8 3 -");
    }

    @Test
    void emptyInputChainIsRejected() {
        assertThatThrownBy(() -> runner.run(transducer, List.of()))
            .isInstanceOf(TranslationException.class)
            .hasMessageContaining("empty");
    }

    @ParameterizedTest(name = "'{0}' is rejected: {1}")
    @MethodSource("invalidExpressions")
    void rejectsInvalidExpressions(String input, String expectedMessagePart, Integer expectedPosition) {
        assertThatThrownBy(() -> translate(input))
            .isInstanceOf(TranslationException.class)
            .hasMessageContaining(expectedMessagePart)
            .extracting("position")
            .isEqualTo(expectedPosition);
    }

    Stream<org.junit.jupiter.params.provider.Arguments> invalidExpressions() {
        return Stream.of(
            // lexical error: symbol outside the alphabet
            org.junit.jupiter.params.provider.Arguments.of("5 @ 2", "not in the input alphabet", 2),
            org.junit.jupiter.params.provider.Arguments.of("5 + a", "not in the input alphabet", 4),
            // empty expression
            org.junit.jupiter.params.provider.Arguments.of("", "empty", 0),
            // missing operand: operator at the beginning of the expression
            org.junit.jupiter.params.provider.Arguments.of("+ 5", "no transition is defined", 0),
            // missing operand: two numbers in a row
            org.junit.jupiter.params.provider.Arguments.of("5 3 +", "lexeme '3'", 2),
            // missing operand: operator after operator
            org.junit.jupiter.params.provider.Arguments.of("5 + * 3", "lexeme '*'", 4),
            org.junit.jupiter.params.provider.Arguments.of("5**3", "lexeme '*'", 2),
            // end of input: the last operation has no right operand
            org.junit.jupiter.params.provider.Arguments.of("5 +", "'⊥'", 3),
            // unbalanced parentheses
            org.junit.jupiter.params.provider.Arguments.of("(5 + 3", "not a final state", 6),
            org.junit.jupiter.params.provider.Arguments.of("((5+3)", "no transition is defined", 6),
            org.junit.jupiter.params.provider.Arguments.of("(5 + 3))", "lexeme ')'", 7),
            // empty parentheses / closing without opening
            org.junit.jupiter.params.provider.Arguments.of("()", "lexeme ')'", 1),
            org.junit.jupiter.params.provider.Arguments.of(")", "no transition is defined", 0),
            // unary minus is not in the language
            org.junit.jupiter.params.provider.Arguments.of("5 * -3", "lexeme '-'", 4)
        );
    }

    @Test
    @DisplayName("a configuration looping on λ-transitions hits the step limit")
    void lambdaLoopHitsStepLimit() {
        String json = """
            {
              "states": ["q0"],
              "inputAlphabet": ["a"],
              "stackAlphabet": ["Z"],
              "outputAlphabet": ["x"],
              "startState": "q0",
              "startStackSymbol": "Z",
              "finalStates": ["q0"],
              "transitions": [
                { "state": "q0", "input": "λ", "stackTop": "Z", "newState": "q0", "push": "Z", "output": "λ" }
              ]
            }
            """;
        Transducer looping = TransducerParser.parse(json);
        List<Lexeme> input = List.of(new Lexeme("a", "a", 0), new Lexeme(Transducer.INPUT_END, Transducer.INPUT_END, 1));
        assertThatThrownBy(() -> runner.run(looping, input))
            .isInstanceOf(TranslationException.class)
            .hasMessageContaining("step limit");
    }
}
