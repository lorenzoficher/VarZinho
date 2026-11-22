package br.edu.unipampa.varzinho.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/** VarZinho's complete visual vocabulary and light/dark theme switch. */
public final class VarZinhoTheme {
    public static final Color SIDEBAR = Color.decode("#0B2632");
    public static final Color SIDEBAR_FOREGROUND = Color.decode("#F5F7FA");
    public static final Color SIDEBAR_MUTED = Color.decode("#9FB2B8");
    public static final Color CONTENT_LIGHT = Color.decode("#F3F7F6");
    public static final Color CONTENT_DARK = Color.decode("#172328");
    public static final Color ACCENT = Color.decode("#C8F35C");
    public static final Color ON_ACCENT = Color.decode("#062A32");
    public static final Color EMERALD = Color.decode("#087F5B");
    public static final Color RECORDING = Color.decode("#D64545");
    public static final Color STOPPED = Color.decode("#6B7280");
    public static final Color MAINTENANCE = Color.decode("#7A4FD6");

    public static final int TITLE_FONT_SIZE = 18;
    public static final int PROMINENT_ACTION_FONT_SIZE = 20;
    public static final int COMPONENT_ARC = 14;
    public static final int BUTTON_ARC = 14;
    public static final int POPUP_ARC = 14;

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
        action.putClientProperty("JButton.buttonType", "default");
        action.putClientProperty("FlatLaf.style", "background: $VarZinho.accent; "
                + "foreground: $VarZinho.onAccent; borderWidth: 0; arc: 16");
    }

    static void styleCard(JComponent component) {
        component.putClientProperty("FlatLaf.style", "arc: 18; background: $VarZinho.cardBackground");
        component.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    }

    static void styleEyebrow(JLabel label) {
        label.putClientProperty("FlatLaf.style", "foreground: $VarZinho.emerald");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
    }

    static JPanel tile(javax.swing.Icon icon) {
        JPanel tile = new JPanel(new java.awt.GridBagLayout());
        tile.putClientProperty("FlatLaf.style", "arc: 16; background: $VarZinho.iconSurface");
        tile.add(new JLabel(icon));
        return tile;
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
        Font systemFont = UIManager.getFont("Label.font");
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String family = java.util.stream.Stream.of("DM Sans", "Segoe UI")
                .filter(name -> java.util.Arrays.asList(installed).contains(name))
                .findFirst().orElse(systemFont.getFamily());
        UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource(family, Font.PLAIN, 13));
        UIManager.put("Panel.background", new javax.swing.plaf.ColorUIResource(content));
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Button.default.background", ACCENT);
        UIManager.put("Button.default.foreground", ON_ACCENT);
        UIManager.put("Button.default.focusColor", ACCENT);
        UIManager.put("Component.arc", COMPONENT_ARC);
        UIManager.put("Button.arc", BUTTON_ARC);
        UIManager.put("TextComponent.arc", COMPONENT_ARC);
        UIManager.put("PopupMenu.borderCornerRadius", POPUP_ARC);
        UIManager.put("VarZinho.sidebarBackground", SIDEBAR);
        UIManager.put("VarZinho.sidebarForeground", SIDEBAR_FOREGROUND);
        UIManager.put("VarZinho.iconSurface", dark ? Color.decode("#25453F") : Color.decode("#E6F4EF"));
        UIManager.put("VarZinho.selectedSurface", dark ? Color.decode("#1E3933") : Color.decode("#F5FBF8"));
        UIManager.put("VarZinho.divider", dark ? Color.decode("#435956") : Color.decode("#DCE7E4"));
        UIManager.put("VarZinho.cardBackground", dark ? Color.decode("#213138") : Color.WHITE);
        UIManager.put("VarZinho.contentBackground", content);
        UIManager.put("VarZinho.accent", ACCENT);
        UIManager.put("VarZinho.onAccent", ON_ACCENT);
        UIManager.put("VarZinho.emerald", dark ? Color.decode("#76DCB9") : EMERALD);
        UIManager.put("VarZinho.recording", dark ? Color.decode("#FF9B96") : Color.decode("#AA3735"));
        UIManager.put("VarZinho.stopped", dark ? Color.decode("#B6C6C9") : Color.decode("#586D75"));
        UIManager.put("VarZinho.maintenance", dark ? Color.decode("#FFD591") : Color.decode("#985709"));
    }
}
