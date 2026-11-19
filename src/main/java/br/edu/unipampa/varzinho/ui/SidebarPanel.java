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
    enum Page { COURTS, ARCHIVE }

    private final JToggleButton courts = new JToggleButton("Quadras", VarZinhoIcons.courts());
    private final JToggleButton archive = new JToggleButton("Acervo", VarZinhoIcons.archive());

    SidebarPanel(String gymName, OperatorView operator, Consumer<Page> onPageChange) {
        super(new MigLayout("insets 24 18, fillx, wrap 1", "[grow,fill]", "[][]32[][]push[]16[][]"));
        if (gymName == null || gymName.isBlank()) throw new IllegalArgumentException("a sidebar needs the gym name");
        if (operator == null) throw new IllegalArgumentException("a sidebar needs the signed-in operator");
        if (onPageChange == null) throw new IllegalArgumentException("a sidebar needs a page destination");
        setPreferredSize(new Dimension(235, 0));
        putClientProperty("FlatLaf.style", "background: $VarZinho.sidebarBackground");

        JLabel product = new JLabel("VarZinho Arena");
        product.setForeground(UIManager.getColor("VarZinho.sidebarForeground"));
        VarZinhoTheme.styleTitle(product);
        add(product, "h 28!");
        JLabel arena = sidebarLabel(gymName);
        arena.setForeground(VarZinhoTheme.ACCENT);
        arena.setFont(arena.getFont().deriveFont(Font.BOLD, 11f));
        add(arena);

        ButtonGroup pages = new ButtonGroup();
        pages.add(courts);
        pages.add(archive);
        courts.setSelected(true);
        styleNavigation(courts);
        styleNavigation(archive);
        courts.addActionListener(event -> onPageChange.accept(Page.COURTS));
        archive.addActionListener(event -> onPageChange.accept(Page.ARCHIVE));
        add(courts, "h 46!");
        add(archive, "h 46!");

        javax.swing.JButton theme = VarZinhoWindow.buildThemeButton();
        theme.putClientProperty("FlatLaf.style", "background: $VarZinho.sidebarBackground; "
                + "foreground: $VarZinho.sidebarForeground; borderWidth: 1; arc: 12");
        add(theme, "h 42!");
        JLabel operatorName = sidebarLabel(operator.getName());
        operatorName.setFont(operatorName.getFont().deriveFont(Font.BOLD));
        add(operatorName);
        add(sidebarLabel("@" + operator.getUsername()));
    }

    JToggleButton courtsButton() { return courts; }
    JToggleButton archiveButton() { return archive; }

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
}
