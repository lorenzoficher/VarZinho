package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.util.function.IntConsumer;

/** Clickable overview of every registered court shown below court registration. */
final class CourtOverviewPanel extends JPanel {
    private final ManageGym management;
    private final IntConsumer selectCourt;
    private final JPanel cards = new JPanel(new GridLayout(1, 0, 12, 0));

    CourtOverviewPanel(ManageGym management, IntConsumer selectCourt) {
        super(new MigLayout("insets 8 0 0 0, fillx, wrap 1", "[grow,fill]", "[]8[]16[]"));
        if (management == null) throw new IllegalArgumentException("a court overview needs gym management");
        if (selectCourt == null) throw new IllegalArgumentException("a court overview needs court selection");
        this.management = management;
        this.selectCourt = selectCourt;
        setOpaque(false);
        JLabel eyebrow = new JLabel("ESTRUTURA");
        VarZinhoTheme.styleEyebrow(eyebrow);
        JLabel title = new JLabel("Suas quadras");
        VarZinhoTheme.styleTitle(title);
        add(eyebrow);
        add(title);
        cards.setOpaque(false);
        add(cards, "growx, h 92!");
        refresh();
    }

    void refresh() {
        cards.removeAll();
        for (CourtView court : management.courts()) {
            JButton card = new JButton("<html><b>Quadra " + court.getNumber() + "</b><br>"
                    + court.getCameras().size() + " câmeras · "
                    + court.getSecondsRecorded() + " s no buffer</html>",
                    VarZinhoIcons.court(VarZinhoTheme.EMERALD));
            card.setHorizontalAlignment(JButton.LEFT);
            card.setIconTextGap(14);
            card.putClientProperty("FlatLaf.style",
                    "background: $VarZinho.cardBackground; arc: 16; borderWidth: 1");
            card.addActionListener(event -> selectCourt.accept(court.getNumber()));
            cards.add(card);
        }
        cards.revalidate();
        cards.repaint();
    }
}
