package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VarZinhoThemeTest {
    /** WCAG 2.2, 1.4.3: body text needs 4.5:1 against its background. */
    private static final double TEXT_MINIMUM = 4.5;

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

        VarZinhoTheme.styleButton(action, ButtonRole.PROMINENT);

        assertTrue(Contrast.focusWidth(action) > 0, "without an outer ring the focus hides in the accent");
        Color ring = Contrast.focusColor(action);
        assertContrasts(ring, UIManager.getColor("VarZinho.contentBackground"), "focus ring on the page");
        assertContrasts(ring, VarZinhoTheme.EMERALD, "focus ring around an emerald button");
    }

    @ParameterizedTest(name = "dark theme: {0}")
    @ValueSource(booleans = {false, true})
    void destructiveActionsReadAgainstPagesAndCards(boolean dark) {
        installTheme(dark);
        Color danger = UIManager.getColor("VarZinho.danger");

        assertTrue(Contrast.ratio(danger, UIManager.getColor("VarZinho.contentBackground")) >= TEXT_MINIMUM);
        assertTrue(Contrast.ratio(danger, UIManager.getColor("VarZinho.cardBackground")) >= TEXT_MINIMUM);
    }

    @ParameterizedTest(name = "dark theme: {0}")
    @ValueSource(booleans = {false, true})
    void statusBadgeTextReadsOnEveryBadge(boolean dark) {
        installTheme(dark);

        for (StatusBadge badge : StatusBadge.values()) {
            double ratio = Contrast.ratio(badge.textColor(), badge.color());
            assertTrue(ratio >= TEXT_MINIMUM, badge + " text has contrast " + ratio);
        }
    }

    @Test
    void destructiveActionsDoNotBorrowTheRecordingRed() {
        for (boolean dark : new boolean[]{false, true}) {
            installTheme(dark);
            assertFalse(UIManager.getColor("VarZinho.danger").equals(UIManager.getColor("VarZinho.recording")));
        }
    }

    @Test
    void everyEverydayButtonRoleIsDrawnAtTheSameHeight() {
        VarZinhoTheme.install();
        List<String> sizes = new ArrayList<>();
        for (ButtonRole role : List.of(ButtonRole.PRIMARY, ButtonRole.SECONDARY, ButtonRole.DESTRUCTIVE)) {
            JButton button = new JButton("Remover quadra");
            VarZinhoTheme.styleButton(button, role);
            // A ring outside the button takes its room from inside the cell, so the visible
            // button is only as tall as the cell minus that ring.
            sizes.add(button.getPreferredSize().height + " high, ring " + Contrast.focusWidth(button) + " outside");
        }

        assertEquals(1, sizes.stream().distinct().count(), "buttons differ: " + sizes);
    }

    @ParameterizedTest(name = "dark theme: {0}")
    @ValueSource(booleans = {false, true})
    void primaryActionRingsItsFocusAgainstTheAccent(boolean dark) {
        installTheme(dark);
        JButton action = new JButton("Entrar");

        VarZinhoTheme.styleButton(action, ButtonRole.PRIMARY);

        assertContrasts(Contrast.focusedBorderColor(action), VarZinhoTheme.ACCENT, "focus ring inside the accent");
    }

    @Test
    void aStyledButtonRemembersItsRole() {
        JButton button = new JButton("Excluir gravação");

        VarZinhoTheme.styleButton(button, ButtonRole.DESTRUCTIVE);

        assertEquals(java.util.Optional.of(ButtonRole.DESTRUCTIVE), VarZinhoTheme.roleOf(button));
        assertEquals(java.util.Optional.empty(), VarZinhoTheme.roleOf(new JButton("sem papel")));
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
