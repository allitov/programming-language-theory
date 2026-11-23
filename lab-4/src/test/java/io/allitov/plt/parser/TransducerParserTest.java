package io.allitov.plt.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.allitov.plt.parsing.TransducerParser;
import io.allitov.plt.transducer.Transducer;
import io.allitov.plt.transducer.Transition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.allitov.plt.exception.ConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransducerParser: json configuration -> model")
class TransducerParserTest {

    private static final String VALID = """
        {
          "states": ["q0", "qf"],
          "inputAlphabet": ["a", "b"],
          "stackAlphabet": ["Z", "A"],
          "outputAlphabet": ["x", "num"],
          "startState": "q0",
          "startStackSymbol": "Z",
          "finalStates": ["qf"],
          "transitions": [
            { "state": "q0", "input": "a", "stackTop": "Z", "newState": "qf", "push": "AZ", "output": "xnum" },
            { "state": "qf", "input": "λ", "stackTop": "Z", "newState": "qf", "push": "λ", "output": "λ" }
          ]
        }
        """;

    @Test
    void parsesValidConfiguration() {
        Transducer transducer = TransducerParser.parse(VALID);
        assertThat(transducer.getStates()).containsExactly("q0", "qf");
        assertThat(transducer.getInputAlphabet()).containsExactlyInAnyOrder("a", "b");
        assertThat(transducer.getStackAlphabet()).containsExactlyInAnyOrder("Z", "A");
        assertThat(transducer.getOutputAlphabet()).containsExactlyInAnyOrder("x", "num");
        assertThat(transducer.getStartState()).isEqualTo("q0");
        assertThat(transducer.getStartStackSymbol()).isEqualTo("Z");
        assertThat(transducer.getFinalStates()).containsExactly("qf");
        assertThat(transducer.getTransitions()).hasSize(2);
        Transition push = transducer.transition("q0", "a", "Z").orElseThrow();
        assertThat(push.push()).containsExactly("A", "Z");
        assertThat(push.output()).containsExactly("x", "num");
        Transition lambda = transducer.transition("qf", "λ", "Z").orElseThrow();
        assertThat(lambda.push()).isEmpty();
        assertThat(lambda.output()).isEmpty();
    }

    @Test
    void malformedJsonIsRejected() {
        assertThatThrownBy(() -> TransducerParser.parse("{ definitely not json"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("cannot parse");
    }

    @Test
    void emptyJsonIsRejected() {
        assertThatThrownBy(() -> TransducerParser.parse("null"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("empty");
    }

    @ParameterizedTest(name = "missing '{0}' is reported")
    @MethodSource("missingFieldConfigurations")
    void missingRequiredFieldsAreReported(String json, String fieldName) {
        assertThatThrownBy(() -> TransducerParser.parse(json))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(fieldName);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> missingFieldConfigurations() {
        String template = """
            {
              "states": ["q0", "qf"],
              "inputAlphabet": ["a"],
              "stackAlphabet": ["Z"],
              "outputAlphabet": ["x"],
              "startState": "q0",
              "startStackSymbol": "Z",
              "finalStates": ["qf"],
              "transitions": [
                { "state": "q0", "input": "a", "stackTop": "Z", "newState": "qf", "push": "Z", "output": "λ" }
              ]
            }
            """;
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(template.replace("\"states\": [\"q0\", \"qf\"],", ""), "states"),
            org.junit.jupiter.params.provider.Arguments.of(template.replace("\"startState\": \"q0\",", ""), "startState"),
            org.junit.jupiter.params.provider.Arguments.of(template.replace("\"startStackSymbol\": \"Z\",", ""), "startStackSymbol"),
            org.junit.jupiter.params.provider.Arguments.of(template.replace("\"finalStates\": [\"qf\"],", ""), "finalStates"),
            org.junit.jupiter.params.provider.Arguments.of(template.replace("\"transitions\"", "\"transitionsX\""), "transitions")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidSymbolConfigurations")
    void symbolsOutsideDeclaredAlphabetsAreRejected(String json, String expectedMessagePart) {
        assertThatThrownBy(() -> TransducerParser.parse(json))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(expectedMessagePart);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> invalidSymbolConfigurations() {
        String base = """
            {
              "states": ["q0", "qf"],
              "inputAlphabet": ["a"],
              "stackAlphabet": ["Z", "A"],
              "outputAlphabet": ["x"],
              "startState": "q0",
              "startStackSymbol": "Z",
              "finalStates": ["qf"],
              "transitions": [
                %s
              ]
            }
            """;
        String rule = "{ \"state\": \"%s\", \"input\": \"%s\", \"stackTop\": \"%s\", \"newState\": \"%s\", \"push\": \"%s\", \"output\": \"%s\" }";
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(base.formatted(rule.formatted("q0", "b", "Z", "qf", "Z", "λ")),
                "input alphabet"),
            org.junit.jupiter.params.provider.Arguments.of(base.formatted(rule.formatted("q0", "a", "X", "qf", "Z", "λ")),
                "stack alphabet"),
            org.junit.jupiter.params.provider.Arguments.of(base.formatted(rule.formatted("q0", "a", "Z", "qf", "BZ", "λ")),
                "push chain 'BZ'"),
            org.junit.jupiter.params.provider.Arguments.of(base.formatted(rule.formatted("q0", "a", "Z", "qf", "Z", "y")),
                "output chain 'y'"),
            org.junit.jupiter.params.provider.Arguments.of(base.formatted(rule.formatted("qX", "a", "Z", "qf", "Z", "λ")),
                "not declared in states"),
            org.junit.jupiter.params.provider.Arguments.of(base.formatted(rule.formatted("q0", "a", "Z", "qX", "Z", "λ")),
                "not declared in states")
        );
    }

    @Test
    void duplicateTransitionKeyIsRejected() {
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
                { "state": "q0", "input": "a", "stackTop": "Z", "newState": "q0", "push": "Z", "output": "λ" },
                { "state": "q0", "input": "a", "stackTop": "Z", "newState": "q0", "push": "λ", "output": "λ" }
              ]
            }
            """;
        assertThatThrownBy(() -> TransducerParser.parse(json))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("more than once");
    }

    @Test
    void lambdaRuleConflictingWithRealRuleIsRejected() {
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
                { "state": "q0", "input": "a", "stackTop": "Z", "newState": "q0", "push": "Z", "output": "λ" },
                { "state": "q0", "input": "λ", "stackTop": "Z", "newState": "q0", "push": "λ", "output": "λ" }
              ]
            }
            """;
        assertThatThrownBy(() -> TransducerParser.parse(json))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("conflicts with a transition by a real input symbol");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("referenceViolations")
    void startAndFinalStateViolationsAreRejected(String json, String expectedMessagePart) {
        assertThatThrownBy(() -> TransducerParser.parse(json))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(expectedMessagePart);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> referenceViolations() {
        String template = """
            {
              "states": ["q0", "qf"],
              "inputAlphabet": ["a"],
              "stackAlphabet": ["Z"],
              "outputAlphabet": ["x"],
              "startState": "%s",
              "startStackSymbol": "%s",
              "finalStates": ["%s"],
              "transitions": [
                { "state": "q0", "input": "a", "stackTop": "Z", "newState": "qf", "push": "Z", "output": "λ" }
              ]
            }
            """;
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(template.formatted("qX", "Z", "qf"), "start state 'qX'"),
            org.junit.jupiter.params.provider.Arguments.of(template.formatted("q0", "A", "qf"), "start stack symbol 'A'"),
            org.junit.jupiter.params.provider.Arguments.of(template.formatted("q0", "Z", "qX"), "final state 'qX'")
        );
    }

    @Test
    void parsesFileAndReportsMissingFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("transducer.json");
        Files.writeString(file, VALID);
        assertThat(TransducerParser.parse(file).getTransitions()).hasSize(2);
        assertThatThrownBy(() -> TransducerParser.parse(tempDir.resolve("absent.json")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("does not exist");
    }
}
