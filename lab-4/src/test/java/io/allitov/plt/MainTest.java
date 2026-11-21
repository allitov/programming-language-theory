package io.allitov.plt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.allitov.plt.console.Console;
import io.allitov.plt.transducer.Transducer;
import io.allitov.plt.parsing.TransducerParser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("Main session: console loop, step output and error handling")
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainTest {

    @Mock
    private Console console;

    private Transducer transducer;

    @BeforeAll
    void setUp() throws IOException {
        transducer = TransducerParser.parse(Path.of("transducer.json"));
    }

    @Test
    void sessionPrintsEveryTickAndTheResult() {
        Main.runSession(console, new Scanner("5 + 3* 2\n"), transducer);
        verify(console).println(eq("Translating: 5 + 3* 2"));
        verify(console).println(startsWith("1) (q0, 5 + 3 * 2 ⊥, Z, λ) ->"));
        verify(console).println(startsWith("11) (q7, λ, Z, 5 3 2 * +) ->"));
        verify(console).println(eq("RPN: 5 3 2 * +"));
        verify(console).println(contains("ACCEPTED"));
        verify(console).println(eq("Bye!"));
    }

    @Test
    void sessionFinishesOnExitCommand() {
        Main.runSession(console, new Scanner("exit\n"), transducer);
        verify(console, never()).println(startsWith("1)"));
        verify(console, never()).println(startsWith("RPN:"));
        verify(console).println(eq("Bye!"));
    }

    @Test
    void errorInOneLineDoesNotStopTheSession() {
        assertDoesNotThrow(() -> Main.runSession(console, new Scanner("5 @ 2\n5+3\nexit\n"), transducer));
        verify(console).println(startsWith("Error:"));
        verify(console).println(eq("RPN: 5 3 +"));
    }

    @Test
    void sessionFinishesOnEmptyLine() {
        Main.runSession(console, new Scanner("5+3\n\n7+7\n"), transducer);
        verify(console).println(eq("RPN: 5 3 +"));
        verify(console, never()).println(eq("RPN: 7 7 +"));
        verify(console).println(eq("Bye!"));
    }
}
