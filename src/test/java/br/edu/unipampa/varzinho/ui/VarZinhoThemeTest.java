package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VarZinhoThemeTest {

    @AfterEach
    void leaveTheNextTestInTheDefaultLightTheme() {
        VarZinhoTheme.install();
    }

    @Test
    void installsTheVarZinhoLightThemeByDefault() {
        VarZinhoTheme.install();

        assertFalse(VarZinhoTheme.isDark());
        assertEquals(VarZinhoTheme.CONTENT_LIGHT, UIManager.getColor("Panel.background"));
        assertEquals(VarZinhoTheme.ACCENT, UIManager.getColor("VarZinho.accent"));
        assertEquals(VarZinhoTheme.COMPONENT_ARC, UIManager.getInt("Component.arc"));
    }

    @Test
    void switchesToDarkAndBackWithoutRememberingTheChoice() {
        VarZinhoTheme.install();

        VarZinhoTheme.toggle();
        assertTrue(VarZinhoTheme.isDark());
        assertEquals(VarZinhoTheme.CONTENT_DARK, UIManager.getColor("Panel.background"));

        VarZinhoTheme.install();
        assertFalse(VarZinhoTheme.isDark());
    }
}
