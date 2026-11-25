package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.Color;

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
    void togglesToDarkAndInstallRestoresLight() {
        VarZinhoTheme.install();

        VarZinhoTheme.toggle();
        assertTrue(VarZinhoTheme.isDark());
        assertEquals(VarZinhoTheme.CONTENT_DARK, UIManager.getColor("Panel.background"));

        VarZinhoTheme.install();
        assertFalse(VarZinhoTheme.isDark());
    }

    @ParameterizedTest(name = "dark theme: {0}")
    @ValueSource(booleans = {false, true})
    void focusRingStandsOutFromPagesAndCards(boolean dark) {
        installTheme(dark);

        for (String key : new String[]{"Component.focusedBorderColor", "Button.focusedBorderColor"}) {
            Color focus = UIManager.getColor(key);
            assertContrasts(focus, UIManager.getColor("VarZinho.contentBackground"), key + " on the page");
            assertContrasts(focus, UIManager.getColor("VarZinho.cardBackground"), key + " on a card");
        }
    }

    @ParameterizedTest(name = "dark theme: {0}")
    @ValueSource(booleans = {false, true})
    void prominentActionRingsItsFocusOutsideTheAccent(boolean dark) {
        installTheme(dark);
        JButton action = new JButton("Salvar lance");

        VarZinhoTheme.styleProminentAction(action);

        assertTrue(Contrast.focusWidth(action) > 0, "without an outer ring the focus hides in the accent");
        Color ring = Contrast.focusColor(action);
        assertContrasts(ring, UIManager.getColor("VarZinho.contentBackground"), "focus ring on the page");
        assertContrasts(ring, VarZinhoTheme.EMERALD, "focus ring around an emerald button");
    }

    static void installTheme(boolean dark) {
        VarZinhoTheme.install();
        if (dark) VarZinhoTheme.toggle();
    }

    static void assertContrasts(Color focus, Color surface, String where) {
        double ratio = Contrast.ratio(focus, surface);
        assertTrue(ratio >= Contrast.FOCUS_MINIMUM, where + " has contrast " + ratio);
    }
}
