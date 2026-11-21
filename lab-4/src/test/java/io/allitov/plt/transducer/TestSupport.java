package io.allitov.plt.transducer;

import java.io.IOException;
import java.nio.file.Path;

/** Loads the RPN transducer configuration used by the engine-level tests. */
final class TestSupport {

    private TestSupport() {
    }

    static Transducer rpnTransducer() {
        try {
            return TransducerParser.parse(Path.of("transducer.json"));
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load transducer.json: " + exception.getMessage(), exception);
        }
    }
}
