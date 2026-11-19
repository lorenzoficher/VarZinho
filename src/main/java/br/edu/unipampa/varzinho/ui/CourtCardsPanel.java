package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;

/** Court selection for a host that supplies snapshots and handles operator actions on the EDT. */
final class CourtCardsPanel extends JPanel {
    private static final int GAP = 10;
    private static final int CARD_WIDTH = 240;
    private final GridLayout layout = new GridLayout(0, 1, GAP, GAP);
    private final JPanel cards = new JPanel(layout);
    private final JLabel empty = new JLabel("Nenhuma quadra cadastrada.");
    private final ButtonGroup selection = new ButtonGroup();
    private final Map<Integer, CourtCard> byNumber = new HashMap<>();
    private final IntConsumer onSelection;

    CourtCardsPanel(IntConsumer onSelection) {
        super(new BorderLayout(0, GAP));
        this.onSelection = Objects.requireNonNull(onSelection, "a court selection needs a listener");
        JLabel title = new JLabel("Suas quadras");
        VarZinhoTheme.styleTitle(title);
        add(title, BorderLayout.NORTH);
        cards.setOpaque(false);
        add(cards, BorderLayout.CENTER);
        cards.add(empty);
        cards.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateColumns();
            }
        });
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(UIManager.getColor("Panel.background"));
    }

    void refresh(List<CourtView> courts) {
        Set<Integer> removed = new HashSet<>(byNumber.keySet());
        if (!courts.isEmpty() && empty.getParent() == cards) cards.remove(empty);
        int index = 0;
        for (CourtView court : courts) {
            int number = court.getNumber();
            removed.remove(number);
            CourtCard card = byNumber.computeIfAbsent(number, this::addCourt);
            card.showCounts(court);
            if (cards.getComponentZOrder(card) != index) cards.setComponentZOrder(card, index);
            index++;
        }
        for (int number : removed) {
            CourtCard card = byNumber.remove(number);
            selection.remove(card);
            cards.remove(card);
        }
        if (courts.isEmpty() && empty.getParent() == null) cards.add(empty);
        updateColumns();
        cards.revalidate();
        cards.repaint();
    }

    /** Synchronizes with the host's selection without firing a user action. */
    void selectCourt(int number) {
        CourtCard card = byNumber.get(number);
        if (card == null) selection.clearSelection();
        else card.setSelected(true);
    }

    private CourtCard addCourt(int number) {
        CourtCard card = new CourtCard(number);
        selection.add(card);
        card.addActionListener(event -> onSelection.accept(number));
        cards.add(card);
        return card;
    }

    private void updateColumns() {
        int available = Math.max(1, (cards.getWidth() + GAP) / (CARD_WIDTH + GAP));
        int columns = Math.max(1, Math.min(Math.min(available, 3), byNumber.size()));
        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
            cards.revalidate();
        }
    }

    private static final class CourtCard extends JToggleButton {
        private final String title;
        private String summary = "";

        CourtCard(int number) {
            title = "Quadra " + number;
            getAccessibleContext().setAccessibleName(title);
            setIcon(VarZinhoIcons.courts());
            if (getIcon() instanceof FlatSVGIcon icon) {
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(ink -> getForeground()));
            }
            setHorizontalAlignment(LEFT);
            setIconTextGap(12);
            setMargin(new Insets(12, 16, 12, 16));
            addItemListener(event -> updateLabel());
        }

        void showCounts(CourtView court) {
            int total = court.getCameraCount();
            String counts = total == 0 ? "Sem câmeras instaladas" : court.getRecordingCameraCount()
                    + " de " + total + (total == 1 ? " câmera gravando" : " câmeras gravando");
            if (!counts.equals(summary)) {
                summary = counts;
                getAccessibleContext().setAccessibleDescription(summary);
                updateLabel();
            }
        }

        private void updateLabel() {
            String marker = isSelected() ? " · Selecionada" : "";
            setText("<html><b>" + title + "</b>" + marker + "<br>" + summary + "</html>");
        }
    }
}
