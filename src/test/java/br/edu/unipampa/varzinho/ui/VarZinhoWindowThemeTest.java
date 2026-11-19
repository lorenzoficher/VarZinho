package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VarZinhoWindowThemeTest {

    @AfterEach
    void restoreLightTheme() throws Exception {
        SwingUtilities.invokeAndWait(VarZinhoTheme::install);
    }

    @Test
    void themeButtonSwitchesDarkAndLightOnTheEventDispatchThread() throws Exception {
        JButton[] button = new JButton[1];
        SwingUtilities.invokeAndWait(() -> {
            VarZinhoTheme.install();
            button[0] = VarZinhoWindow.buildThemeButton();
            button[0].doClick();
            assertTrue(VarZinhoTheme.isDark());
            button[0].doClick();
        });

        assertFalse(VarZinhoTheme.isDark());
    }
}
