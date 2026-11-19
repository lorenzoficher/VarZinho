package br.edu.unipampa.varzinho.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;

/** The Tabler icons used by VarZinho, loaded from the licensed bundled resources. */
final class VarZinhoIcons {
    private static final String ROOT = "icons/tabler/";
    static final int SIZE = 18;

    private VarZinhoIcons() { }

    static Icon courts() { return icon("building-stadium"); }
    static Icon archive() { return icon("archive"); }
    static Icon operators() { return icon("users"); }
    static Icon record() { return icon("player-record"); }
    static Icon stop() { return icon("player-stop"); }
    static Icon maintenance() { return icon("tool"); }
    static Icon aim() { return icon("focus-2"); }
    static Icon openClip() { return icon("external-link"); }
    static Icon theme() { return icon("sun-moon"); }

    private static Icon icon(String name) {
        return new FlatSVGIcon(ROOT + name + ".svg", SIZE, SIZE);
    }
}
