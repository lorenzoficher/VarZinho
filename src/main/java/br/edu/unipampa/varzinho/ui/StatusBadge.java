package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.enums.CameraStatus;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * The coloured badge a camera's status is shown as. It is chosen from the status itself,
 * never from the words describing it, so a translation cannot change its colour.
 */
enum StatusBadge {
    RECORDING("Gravando", "VarZinho.recording", VarZinhoTheme.RECORDING),
    STOPPED("Parada", "VarZinho.stopped", VarZinhoTheme.STOPPED),
    MAINTENANCE("Em manutenção", "VarZinho.maintenance", VarZinhoTheme.MAINTENANCE);

    private final String label;
    private final String colorKey;
    private final Color fallback;

    StatusBadge(String label, String colorKey, Color fallback) {
        this.label = label;
        this.colorKey = colorKey;
        this.fallback = fallback;
    }

    static StatusBadge of(CameraStatus status) {
        return switch (status) {
            case ACTIVE -> RECORDING;
            case INACTIVE -> STOPPED;
            case MAINTENANCE -> MAINTENANCE;
        };
    }

    String label() {
        return label;
    }

    // Before the theme is installed (as in tests) the key is absent, and the badge keeps its colour.
    Color color() {
        Color themed = UIManager.getColor(colorKey);
        return themed == null ? fallback : themed;
    }
}
