package br.edu.unipampa.varzinho;

import br.edu.unipampa.varzinho.bootstrap.Launcher;

/**
 * Entry point of the VarZinho application.
 *
 * <p>Delegates immediately to {@link br.edu.unipampa.varzinho.bootstrap.Launcher},
 * which wires every dependency before opening the window.
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        Launcher.launch();
    }
}
