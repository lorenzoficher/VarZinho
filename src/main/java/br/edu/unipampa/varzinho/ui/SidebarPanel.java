package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorView;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.Dimension;
import java.util.function.Consumer;

/** Navigation and session identity kept at the left of the operator window. */
final class SidebarPanel extends JPanel {
    enum Page { DASHBOARD, COURTS, CAMERAS, HIGHLIGHTS, ARCHIVE, SETTINGS }

    private final JToggleButton dashboard = navigation("Dashboard", "dashboard");
    private final JToggleButton courts = navigation("Quadras", "courts");
    private final JToggleButton cameras = navigation("Câmeras", "cameras");
    private final JToggleButton highlights = navigation("Melhores momentos", "highlights");
    private final JToggleButton archive = navigation("Arquivos", "archive");
    private final JToggleButton settings = navigation("Configurações", "settings");

    SidebarPanel(String gymName, OperatorView operator, Consumer<Page> onPageChange) {
        super(new MigLayout("insets 24 18, fillx, wrap 1", "[grow,fill]", "[][]32[]8[][][][][][]push[]16[][]"));
        if (gymName == null || gymName.isBlank()) throw new IllegalArgumentException("a sidebar needs the gym name");
        if (operator == null) throw new IllegalArgumentException("a sidebar needs the signed-in operator");
        if (onPageChange == null) throw new IllegalArgumentException("a sidebar needs a page destination");
        setPreferredSize(new Dimension(235, 0));
        putClientProperty("FlatLaf.style", "background: $VarZinho.sidebarBackground");

        JLabel product = new JLabel("VarZinho Arena");
        product.setIcon(VarZinhoIcons.brand(VarZinhoTheme.ACCENT));
        product.setIconTextGap(12);
        product.setForeground(UIManager.getColor("VarZinho.sidebarForeground"));
        VarZinhoTheme.styleTitle(product);
        add(product, "h 28!");
        JLabel arena = sidebarLabel(gymName);
        arena.setForeground(VarZinhoTheme.ACCENT);
        arena.setFont(arena.getFont().deriveFont(Font.BOLD, 11f));
        add(arena);

        JLabel menu = sidebarLabel("MENU PRINCIPAL");
        menu.setForeground(VarZinhoTheme.STOPPED);
        menu.setFont(menu.getFont().deriveFont(Font.BOLD, 10f));
        add(menu);

        ButtonGroup pages = new ButtonGroup();
        pages.add(dashboard);
        pages.add(courts);
        pages.add(cameras);
        pages.add(highlights);
        pages.add(archive);
        pages.add(settings);
        dashboard.setSelected(true);
        styleNavigation(dashboard);
        styleNavigation(courts);
        styleNavigation(cameras);
        styleNavigation(highlights);
        styleNavigation(archive);
        styleNavigation(settings);
        dashboard.addActionListener(event -> onPageChange.accept(Page.DASHBOARD));
        courts.addActionListener(event -> onPageChange.accept(Page.COURTS));
        cameras.addActionListener(event -> onPageChange.accept(Page.CAMERAS));
        highlights.addActionListener(event -> onPageChange.accept(Page.HIGHLIGHTS));
        archive.addActionListener(event -> onPageChange.accept(Page.ARCHIVE));
        settings.addActionListener(event -> onPageChange.accept(Page.SETTINGS));
        add(dashboard, "h 46!");
        add(courts, "h 46!");
        add(cameras, "h 46!");
        add(highlights, "h 54!");
        add(archive, "h 46!");
        add(settings, "h 46!");

        JLabel status = sidebarLabel("●  SISTEMA OPERACIONAL");
        status.setForeground(VarZinhoTheme.ACCENT);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 10f));
        add(status);
        JLabel operatorName = sidebarLabel(operator.getName());
        operatorName.setFont(operatorName.getFont().deriveFont(Font.BOLD));
        add(operatorName);
        add(sidebarLabel("@" + operator.getUsername()));
    }

    JToggleButton dashboardButton() { return dashboard; }
    JToggleButton courtsButton() { return courts; }
    JToggleButton camerasButton() { return cameras; }
    JToggleButton highlightsButton() { return highlights; }
    JToggleButton archiveButton() { return archive; }
    JToggleButton settingsButton() { return settings; }

    void select(Page page) {
        switch (page) {
            case DASHBOARD: dashboard.setSelected(true); break;
            case COURTS: courts.setSelected(true); break;
            case CAMERAS: cameras.setSelected(true); break;
            case HIGHLIGHTS: highlights.setSelected(true); break;
            case ARCHIVE: archive.setSelected(true); break;
            case SETTINGS: settings.setSelected(true); break;
            default: throw new IllegalArgumentException("unknown page " + page);
        }
    }

    private static JLabel sidebarLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIManager.getColor("VarZinho.sidebarForeground"));
        return label;
    }

    private static void styleNavigation(JToggleButton button) {
        button.putClientProperty("FlatLaf.style", "foreground: $VarZinho.sidebarForeground; "
                + "background: $VarZinho.sidebarBackground; selectedBackground: $VarZinho.accent; "
                + "selectedForeground: $VarZinho.sidebarBackground; borderWidth: 0; arc: 12; iconTextGap: 12");
    }

    private static JToggleButton navigation(String text, String icon) {
        JToggleButton button = new JToggleButton(text, icon(icon, VarZinhoTheme.SIDEBAR_MUTED));
        button.setSelectedIcon(icon(icon, VarZinhoTheme.ON_ACCENT));
        return button;
    }

    private static javax.swing.Icon icon(String name, java.awt.Color color) {
        switch (name) {
            case "dashboard": return VarZinhoIcons.dashboard(color);
            case "courts": return VarZinhoIcons.court(color);
            case "cameras": return VarZinhoIcons.cameras(color);
            case "highlights": return VarZinhoIcons.highlights(color);
            case "archive": return VarZinhoIcons.archive(color);
            case "settings": return VarZinhoIcons.settings(color);
            default: throw new IllegalArgumentException("unknown navigation icon " + name);
        }
    }
}
