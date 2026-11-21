package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** The same selectable court summary is used in the dashboard and court management. */
final class CourtOverviewPanel extends JPanel {
    private final GridLayout grid = new GridLayout(0, 3, 12, 12);
    private final JPanel cards = new JPanel(grid);
    private final IntConsumer onSelection;
    private final IntFunction<String> sportOf;
    private final IntFunction<String> nameOf;

    CourtOverviewPanel(IntConsumer onSelection, IntFunction<String> sportOf, IntFunction<String> nameOf, Runnable browse) {
        super(new MigLayout("insets 0, fillx, wrap 2", "[grow,fill][]", "[]2[]9[]"));
        this.onSelection = onSelection;
        this.sportOf = sportOf;
        this.nameOf = nameOf;
        setOpaque(false);
        JLabel eyebrow = new JLabel("ESTRUTURA");
        VarZinhoTheme.styleEyebrow(eyebrow);
        add(eyebrow, "span 2");
        JLabel title = new JLabel("Suas quadras");
        VarZinhoTheme.styleTitle(title);
        add(title);
        if (browse != null) {
            JButton all = new JButton("Ver todas   ›");
            all.putClientProperty("JButton.buttonType", "borderless");
            all.putClientProperty("FlatLaf.style", "foreground: $VarZinho.emerald");
            all.addActionListener(event -> browse.run());
            add(all);
        }
        cards.setOpaque(false);
        add(cards, "newline, span 2, growx");
        cards.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { updateColumns(); }
        });
    }

    void refresh(List<CourtView> courts, int selected) {
        cards.removeAll();
        if (courts.isEmpty()) cards.add(new JLabel("Nenhuma quadra cadastrada."));
        for (CourtView court : courts) cards.add(card(court, court.getNumber() == selected));
        updateColumns();
        revalidate();
        repaint();
    }

    private void updateColumns() {
        int fit = cards.getWidth() == 0 ? 3 : Math.max(1, (cards.getWidth() + 12) / 310);
        int columns = Math.max(1, Math.min(cards.getComponentCount(), Math.min(fit, 3)));
        if (grid.getColumns() != columns) {
            grid.setColumns(columns);
            revalidate();
        }
    }

    private JButton card(CourtView court, boolean selected) {
        int number = court.getNumber();
        String title = nameOf.apply(number);
        String sport = sportOf.apply(number);
        JButton button = new JButton();
        button.setLayout(new MigLayout("insets 10 14, fill", "[44!]12[grow,fill]6[right]8[]", "[]2[]"));
        button.setPreferredSize(new Dimension(300, 80));
        button.putClientProperty("FlatLaf.style", "arc: 24; borderWidth: 1; background: $VarZinho.cardBackground"
                + (selected ? "; borderColor: $VarZinho.emerald; background: $VarZinho.selectedSurface" : "; borderColor: $VarZinho.divider"));
        button.add(VarZinhoTheme.tile(sport == null ? VarZinhoIcons.court(VarZinhoTheme.EMERALD)
                : DashboardPanel.sportIcon(sport, VarZinhoTheme.EMERALD)), "cell 0 0, spany 2, h 44!, growx");
        JLabel name = new JLabel(title);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        button.add(name, "cell 1 0, aligny bottom");
        button.add(new JLabel(sport == null ? "Modalidade a definir" : sport), "cell 1 1, aligny top");
        JLabel state = new JLabel(selected ? "Selecionada" : "Disponível");
        state.putClientProperty("FlatLaf.style", "foreground: $VarZinho.emerald");
        state.setFont(state.getFont().deriveFont(Font.BOLD, 12f));
        button.add(state, "cell 2 0, aligny bottom");
        int total = court.getCameraCount();
        String counts = total == 0 ? "Sem câmeras" : court.getRecordingCameraCount() + " de " + total + " gravando";
        JLabel stat = new JLabel(counts);
        stat.setFont(stat.getFont().deriveFont(12f));
        button.add(stat, "cell 2 1, aligny top");
        button.add(new JLabel("›"), "cell 3 0, spany 2");
        button.setToolTipText("Quadra " + number + " · " + state.getText());
        button.getAccessibleContext().setAccessibleName(title + " · " + state.getText() + " · " + counts);
        button.addActionListener(event -> onSelection.accept(number));
        return button;
    }
}
