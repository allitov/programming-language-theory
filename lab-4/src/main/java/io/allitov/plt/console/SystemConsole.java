package io.allitov.plt.console;

/** Standard-output implementation of {@link Console}. */
public class SystemConsole implements Console {

    @Override
    public void println(String line) {
        System.out.println(line);
    }
}
