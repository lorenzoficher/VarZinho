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
import java.util.Optional;

/**
 * VarZinho's complete visual vocabulary and light/dark theme switch.
 *
 * <p>Every colour, font size and button look the window uses is named here; panels ask for
 * a role ({@link #styleTitle}, {@link #styleButton}) instead of building their own, so all
 * screens read as one product.
 */
public final class VarZinhoTheme {
    /** Dark teal used as the sidebar background. */
    public static final Color SIDEBAR = Color.decode("#0B2632");
    /** Off-white text colour used over the sidebar background. */
    public static final Color SIDEBAR_FOREGROUND = Color.decode("#F5F7FA");
    /** Muted text colour for secondary sidebar labels. */
    public static final Color SIDEBAR_MUTED = Color.decode("#9FB2B8");
    /** Page background in the light theme. */
    public static final Color CONTENT_LIGHT = Color.decode("#F3F7F6");
    /** Page background in the dark theme. */
    public static final Color CONTENT_DARK = Color.decode("#172328");
    /** Lime-green accent used for highlighted actions, and for focus rings on dark surfaces. */
    public static final Color ACCENT = Color.decode("#C8F35C");
    /** Dark text colour drawn over the accent colour. */
    public static final Color ON_ACCENT = Color.decode("#062A32");
    /** Emerald green used for status indicators and eyebrow labels. */
    public static final Color EMERALD = Color.decode("#087F5B");
    /** Red used to signal that a camera is actively recording. */
    public static final Color RECORDING = Color.decode("#D64545");
    /** Grey used to signal that a camera is stopped. */
    public static final Color STOPPED = Color.decode("#6B7280");
    /** Purple used to signal that a camera is in maintenance. */
    public static final Color MAINTENANCE = Color.decode("#7A4FD6");
    /** Text colour drawn over the dark pitch and over the saturated status colours. */
    public static final Color ON_DARK = Color.decode("#FFFFFF");
    /** Deep green the pitch gradient fades into, from the sidebar teal. */
    public static final Color PITCH = Color.decode("#0F4D42");
    /** Translucent white of the court markings drawn over the pitch. */
    public static final Color PITCH_MARKINGS = new Color(255, 255, 255, 38);

    /** Point size of the court name over the pitch, the largest text in the window. */
    public static final int DISPLAY_FONT_SIZE = 26;
    /** Point size of the capture button. */
    public static final int PROMINENT_ACTION_FONT_SIZE = 20;
    /** Point size of page, section and panel titles. */
    public static final int TITLE_FONT_SIZE = 18;
    /** Point size of body text, fields and every everyday button. */
    public static final int BODY_FONT_SIZE = 13;
    /** Point size of captions, statistics and status badges. */
    public static final int CAPTION_FONT_SIZE = 12;
    /** Point size of the small bold labels above a title. */
    public static final int EYEBROW_FONT_SIZE = 11;
    /** Corner-arc radius applied to all components. */
    public static final int COMPONENT_ARC = 14;
    /** Corner-arc radius applied to buttons. */
    public static final int BUTTON_ARC = 14;
    /** Corner-arc radius applied to popup menus. */
    public static final int POPUP_ARC = 14;
    /** MigLayout size of every text field, selector and button in a form: one height. */
    static final String CONTROL_SIZE = "h 38!";
    /** MigLayout column of a form: labels sit above fields 360 to 420 wide, never beside them. */
    static final String FORM_COLUMN = "[360:420:420,grow,fill]";
    /** Width, in pixels, the focus ring adds inside a focused component's border. */
    private static final float FOCUS_RING_WIDTH = 1f;
    /** Client property under which {@link #styleButton} records a button's role. */
    private static final String ROLE_PROPERTY = "VarZinho.buttonRole";
    /** Padding of every everyday button, which is what gives them one height. */
    private static final String BUTTON_MARGIN = "margin: 8,16,8,16; ";

    /**
     * Style fragment for a button filled with a solid colour and drawn without a border.
     * A ring inside the fill would hug the page edge and read as a smaller button, so the
     * focus is a ring outside it, in a colour that stands out from the page and the fill.
     */
    static final String FILLED_FOCUS_RING = "borderWidth: 0; default.borderWidth: 0; focusWidth: 2; "
            + "innerFocusWidth: 0; focusColor: $VarZinho.filledFocus; default.focusColor: $VarZinho.filledFocus";

    private VarZinhoTheme() { }

    /** Installs the light theme on every application run. */
    public static void install() {
        apply(false, false);
    }

    /** Changes every open window between the light and dark themes. */
    public static void toggle() {
        apply(!isDark(), true);
    }

    /**
     * Tells whether the dark theme is currently active, so callers can mirror the
     * palette on their own widgets.
     * @return {@code true} when the dark theme is on, {@code false} on the light one
     */
    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }

    static void styleDisplay(JComponent text) {
        font(text, Font.BOLD, DISPLAY_FONT_SIZE);
    }

    static void styleTitle(JComponent title) {
        font(title, Font.BOLD, TITLE_FONT_SIZE);
    }

    static void styleStrong(JComponent text) {
        font(text, Font.BOLD, BODY_FONT_SIZE);
    }

    static void styleCaption(JComponent text) {
        font(text, Font.PLAIN, CAPTION_FONT_SIZE);
    }

    /** A caption in bold, for a state the eye should catch: "Selecionada", "● Ao vivo". */
    static void styleStatus(JComponent text) {
        font(text, Font.BOLD, CAPTION_FONT_SIZE);
    }

    static void styleEyebrow(JLabel label) {
        styleEyebrow(label, "emerald");
    }

    /**
     * Draws a small bold label above a title, in one of the palette's colours.
     * @param colour the {@code VarZinho.*} palette entry, without the prefix
     */
    static void styleEyebrow(JLabel label, String colour) {
        label.putClientProperty("FlatLaf.style", "foreground: $VarZinho." + colour);
        font(label, Font.BOLD, EYEBROW_FONT_SIZE);
    }

    /**
     * Gives a button the one look its role has on every screen, and remembers the role.
     * Replaces any style the button carried before.
     */
    static void styleButton(AbstractButton button, ButtonRole role) {
        button.putClientProperty(ROLE_PROPERTY, role);
        button.putClientProperty("JButton.buttonType", null);
        switch (role) {
            case PROMINENT -> {
                font(button, Font.BOLD, PROMINENT_ACTION_FONT_SIZE);
                button.putClientProperty("FlatLaf.style", "background: $VarZinho.accent; "
                        + "foreground: $VarZinho.onAccent; arc: 16; " + FILLED_FOCUS_RING);
            }
            case PRIMARY -> {
                font(button, Font.BOLD, BODY_FONT_SIZE);
                // An outer ring would shrink the visible fill below the other buttons, so this
                // one rings its focus inside, in the dark ink it writes on the lime.
                button.putClientProperty("FlatLaf.style", BUTTON_MARGIN + "background: $VarZinho.accent; "
                        + "foreground: $VarZinho.onAccent; borderColor: $VarZinho.accent; "
                        + "hoverBorderColor: $VarZinho.accent; focusedBorderColor: $VarZinho.onAccent; "
                        // The button Enter presses is drawn from the default.* keys; without
                        // them it would turn blue and stand apart from every other primary.
                        + "default.background: $VarZinho.accent; default.foreground: $VarZinho.onAccent; "
                        + "default.borderColor: $VarZinho.accent; default.hoverBorderColor: $VarZinho.accent; "
                        + "default.focusedBorderColor: $VarZinho.onAccent; default.focusColor: $VarZinho.onAccent");
            }
            case SECONDARY -> {
                font(button, Font.PLAIN, BODY_FONT_SIZE);
                button.putClientProperty("FlatLaf.style", BUTTON_MARGIN.strip());
            }
            case DESTRUCTIVE -> {
                font(button, Font.PLAIN, BODY_FONT_SIZE);
                button.putClientProperty("FlatLaf.style", BUTTON_MARGIN
                        + "foreground: $VarZinho.danger; borderColor: $VarZinho.danger");
            }
            case LINK -> {
                font(button, Font.PLAIN, BODY_FONT_SIZE);
                button.putClientProperty("JButton.buttonType", "borderless");
                button.putClientProperty("FlatLaf.style", "foreground: $VarZinho.emerald");
            }
            case TILE -> selectTile(button, false);
            default -> throw new IllegalArgumentException("unknown button role " + role);
        }
    }

    /**
     * Marks a {@link ButtonRole#TILE} as the one currently chosen, or not.
     * @throws IllegalArgumentException if the button is not a tile
     */
    static void selectTile(AbstractButton tile, boolean selected) {
        if (roleOf(tile).orElse(null) != ButtonRole.TILE) {
            throw new IllegalArgumentException("only a tile can be shown as selected");
        }
        tile.putClientProperty("FlatLaf.style", "arc: 24; borderWidth: 1; " + (selected
                ? "background: $VarZinho.selectedSurface; borderColor: $VarZinho.emerald"
                : "background: $VarZinho.cardBackground; borderColor: $VarZinho.divider"));
    }

    /**
     * Tells which role {@link #styleButton} gave a button.
     * @return the role, or empty for a button no role was given
     */
    static Optional<ButtonRole> roleOf(AbstractButton button) {
        Object role = button.getClientProperty(ROLE_PROPERTY);
        return role instanceof ButtonRole known ? Optional.of(known) : Optional.empty();
    }

    /** One of several mutually exclusive choices drawn as a chip, such as the sport. */
    static void styleChoice(AbstractButton choice) {
        choice.putClientProperty("FlatLaf.style", "arc: 14; margin: 7,12,7,12; "
                + "selectedBackground: $VarZinho.sidebarBackground; selectedForeground: $VarZinho.sidebarForeground");
    }

    /** An entry of the sidebar menu. */
    static void styleNavigation(AbstractButton item) {
        item.setHorizontalAlignment(AbstractButton.LEFT);
        styleStrong(item);
        item.putClientProperty("FlatLaf.style", "foreground: $VarZinho.sidebarForeground; "
                + "background: $VarZinho.sidebarBackground; selectedBackground: $VarZinho.sidebarSelected; "
                + "selectedForeground: $VarZinho.sidebarForeground; arc: 12; iconTextGap: 12; "
                // A transparent border keeps the items flat yet leaves room for the focus ring.
                + "borderWidth: 2; borderColor: $VarZinho.transparent; hoverBorderColor: $VarZinho.transparent; "
                + "focusedBorderColor: $VarZinho.accent");
    }

    /** A button that sits on the dark sidebar rather than on a page. */
    static void styleSidebarAction(AbstractButton action) {
        action.putClientProperty("FlatLaf.style", "background: $VarZinho.sidebarBackground; "
                + "foreground: $VarZinho.sidebarForeground; borderColor: $VarZinho.sidebarBorder; "
                + "focusedBorderColor: $VarZinho.accent");
    }

    static void styleSidebarCard(JComponent card) {
        card.putClientProperty("FlatLaf.style", "background: $VarZinho.sidebarCard; arc: 14");
    }

    static void styleCard(JComponent component) {
        component.putClientProperty("FlatLaf.style", "arc: 18; background: $VarZinho.cardBackground");
        component.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    }

    static JPanel tile(javax.swing.Icon icon) {
        JPanel tile = new JPanel(new java.awt.GridBagLayout());
        tile.putClientProperty("FlatLaf.style", "arc: 16; background: $VarZinho.iconSurface");
        tile.add(new JLabel(icon));
        return tile;
    }

    private static void font(JComponent component, int style, int size) {
        component.setFont(component.getFont().deriveFont(style, (float) size));
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
        UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource(family, Font.PLAIN, BODY_FONT_SIZE));
        UIManager.put("Panel.background", new javax.swing.plaf.ColorUIResource(content));
        // The lime accent vanishes on the light page, so the light theme rings focus in
        // emerald; both clear WCAG's 3:1 for a focus indicator on pages and cards.
        Color heading = dark ? SIDEBAR_FOREGROUND : ON_ACCENT;
        UIManager.put("Label.foreground", new javax.swing.plaf.ColorUIResource(heading));
        UIManager.put("TitlePane.foreground", new javax.swing.plaf.ColorUIResource(heading));
        UIManager.put("TitlePane.background", new javax.swing.plaf.ColorUIResource(content));
        Color focus = dark ? ACCENT : EMERALD;
        UIManager.put("Component.focusColor", focus);
        UIManager.put("Component.focusedBorderColor", focus);
        UIManager.put("Component.innerFocusWidth", FOCUS_RING_WIDTH);
        UIManager.put("Button.focusedBorderColor", focus);
        UIManager.put("Button.innerFocusWidth", FOCUS_RING_WIDTH);
        UIManager.put("Button.default.background", ACCENT);
        UIManager.put("Button.default.foreground", ON_ACCENT);
        UIManager.put("Button.default.focusColor", focus);
        UIManager.put("Button.default.focusedBorderColor", ON_ACCENT);
        UIManager.put("Component.arc", COMPONENT_ARC);
        UIManager.put("Button.arc", BUTTON_ARC);
        UIManager.put("TextComponent.arc", COMPONENT_ARC);
        UIManager.put("PopupMenu.borderCornerRadius", POPUP_ARC);
        UIManager.put("VarZinho.sidebarBackground", SIDEBAR);
        UIManager.put("VarZinho.sidebarForeground", SIDEBAR_FOREGROUND);
        UIManager.put("VarZinho.sidebarMuted", SIDEBAR_MUTED);
        UIManager.put("VarZinho.sidebarCard", Color.decode("#16383E"));
        UIManager.put("VarZinho.sidebarBorder", Color.decode("#45606A"));
        UIManager.put("VarZinho.sidebarSelected", Color.decode("#104440"));
        UIManager.put("VarZinho.transparent", new Color(0, 0, 0, 0));
        UIManager.put("VarZinho.iconSurface", dark ? Color.decode("#25453F") : Color.decode("#E6F4EF"));
        UIManager.put("VarZinho.selectedSurface", dark ? Color.decode("#1E3933") : Color.decode("#F5FBF8"));
        UIManager.put("VarZinho.divider", dark ? Color.decode("#435956") : Color.decode("#DCE7E4"));
        UIManager.put("VarZinho.cardBackground", dark ? Color.decode("#213138") : Color.WHITE);
        UIManager.put("VarZinho.contentBackground", content);
        UIManager.put("VarZinho.accent", ACCENT);
        UIManager.put("VarZinho.onAccent", ON_ACCENT);
        UIManager.put("VarZinho.onDark", ON_DARK);
        UIManager.put("VarZinho.onStatus", dark ? CONTENT_DARK : ON_DARK);
        UIManager.put("VarZinho.filledFocus", dark ? SIDEBAR_FOREGROUND : SIDEBAR);
        UIManager.put("VarZinho.emerald", dark ? Color.decode("#76DCB9") : EMERALD);
        UIManager.put("VarZinho.recording", dark ? Color.decode("#FF9B96") : Color.decode("#AA3735"));
        // Removing is not recording: a separate red keeps "delete" from reading as "live".
        UIManager.put("VarZinho.danger", dark ? Color.decode("#FFB4AB") : Color.decode("#B3261E"));
        UIManager.put("VarZinho.stopped", dark ? Color.decode("#B6C6C9") : Color.decode("#586D75"));
        UIManager.put("VarZinho.maintenance", dark ? Color.decode("#FFD591") : Color.decode("#985709"));
    }
}
