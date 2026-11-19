package br.edu.unipampa.varzinho.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/** VarZinho's complete visual vocabulary and light/dark theme switch. */
public final class VarZinhoTheme {
    public static final Color SIDEBAR = Color.decode("#111317");
    public static final Color CONTENT_LIGHT = Color.decode("#FFFFFF");
    public static final Color CONTENT_DARK = Color.decode("#202226");
    public static final Color ACCENT = Color.decode("#F2A900");
    public static final Color ON_ACCENT = Color.decode("#111317");
    public static final Color RECORDING = Color.decode("#D64545");
    public static final Color STOPPED = Color.decode("#6B7280");
    public static final Color MAINTENANCE = Color.decode("#7A4FD6");

    public static final int TITLE_FONT_SIZE = 18;
    public static final int PROMINENT_ACTION_FONT_SIZE = 20;
    public static final int COMPONENT_ARC = 10;
    public static final int BUTTON_ARC = 10;
    public static final int POPUP_ARC = 10;

    private VarZinhoTheme() { }

    /** Installs the light theme on every application run. */
    public static void install() {
        apply(false, false);
    }

    /** Changes every open window between the light and dark themes. */
    public static void toggle() {
        apply(!isDark(), true);
    }

    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }

    static void styleTitle(JLabel title) {
        title.setFont(title.getFont().deriveFont(Font.BOLD, (float) TITLE_FONT_SIZE));
    }

    static void styleProminentAction(AbstractButton action) {
        action.setFont(action.getFont().deriveFont(Font.BOLD, (float) PROMINENT_ACTION_FONT_SIZE));
    }

    private static void apply(boolean dark, boolean animate) {
        boolean snapshot = animate && !GraphicsEnvironment.isHeadless();
        if (snapshot) FlatAnimatedLafChange.showSnapshot();
        try {
            boolean installed = dark ? FlatDarkLaf.setup() : FlatLightLaf.setup();
            if (!installed) throw new IllegalStateException("the VarZinho theme could not be installed");
            applyDefaults(dark);
            FlatLaf.updateUI();
        } finally {
            if (snapshot) FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }

    private static void applyDefaults(boolean dark) {
        Color content = dark ? CONTENT_DARK : CONTENT_LIGHT;
        UIManager.put("Panel.background", content);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Button.default.background", ACCENT);
        UIManager.put("Button.default.foreground", ON_ACCENT);
        UIManager.put("Button.default.focusColor", ACCENT);
        UIManager.put("Component.arc", COMPONENT_ARC);
        UIManager.put("Button.arc", BUTTON_ARC);
        UIManager.put("TextComponent.arc", COMPONENT_ARC);
        UIManager.put("PopupMenu.borderCornerRadius", POPUP_ARC);
        UIManager.put("VarZinho.sidebarBackground", SIDEBAR);
        UIManager.put("VarZinho.contentBackground", content);
        UIManager.put("VarZinho.accent", ACCENT);
        UIManager.put("VarZinho.recording", RECORDING);
        UIManager.put("VarZinho.stopped", STOPPED);
        UIManager.put("VarZinho.maintenance", MAINTENANCE);
    }
}
