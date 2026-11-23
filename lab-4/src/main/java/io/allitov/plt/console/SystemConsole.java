package io.allitov.plt.console;

public class SystemConsole implements Console {

    @Override
    public void println(String line) {
        IO.println(line);
    }
}
