package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;

/** Live arena dashboard composed only from data and actions the application owns. */
final class DashboardPanel extends JPanel {
    private final ManageGym management;
    private final CourtPanel courtPanel;
    private final QuickPtzPanel quickPtz;
    private final JComboBox<Integer> courtSelector = new JComboBox<>();
    private final JLabel courtName = new JLabel();
    private final JLabel courtDetails = new JLabel();
    private final JLabel recording = new JLabel();
    private final JPanel courtCards = new JPanel(new GridLayout(1, 0, 12, 0));
    private boolean refreshing;

    DashboardPanel(ManageGym management, CourtPanel courtPanel, CapturePanel capturePanel,
                   Consumer<SidebarPanel.Page> navigate, Consumer<String> messages) {
        super(new MigLayout("insets 0, fillx, wrap 2", "[grow,fill][390!,fill]", "[]16[220!]28[][]"));
        if (management == null) throw new IllegalArgumentException("a dashboard needs gym management");
        if (courtPanel == null) throw new IllegalArgumentException("a dashboard needs the court selection");
        if (capturePanel == null) throw new IllegalArgumentException("a dashboard needs highlight capture");
        if (navigate == null) throw new IllegalArgumentException("a dashboard needs navigation");
        if (messages == null) throw new IllegalArgumentException("a dashboard needs message feedback");
        this.management = management;
        this.courtPanel = courtPanel;
        this.quickPtz = new QuickPtzPanel(management, courtPanel::selectedCourtNumber, messages);

        JPanel selector = new JPanel(new MigLayout("insets 0, fillx", "[grow][][220!]", "[]"));
        selector.setOpaque(false);
        selector.add(new JLabel("Quadra selecionada"), "skip 1");
        selector.add(courtSelector, "h 42!");
        add(selector, "span 2, growx");

        add(hero(capturePanel.additionalSaveButton()), "grow");
        add(quickPtz, "grow");

        JLabel eyebrow = new JLabel("ESTRUTURA");
        VarZinhoTheme.styleEyebrow(eyebrow);
        add(eyebrow, "span 2, wrap");
        JPanel courtsHeader = new JPanel(new BorderLayout());
        courtsHeader.setOpaque(false);
        JLabel courtsTitle = new JLabel("Suas quadras");
        VarZinhoTheme.styleTitle(courtsTitle);
        JButton seeAll = new JButton("Ver todas →");
        seeAll.putClientProperty("JButton.buttonType", "borderless");
        seeAll.addActionListener(event -> navigate.accept(SidebarPanel.Page.COURTS));
        courtsHeader.add(courtsTitle, BorderLayout.WEST);
        courtsHeader.add(seeAll, BorderLayout.EAST);
        add(courtsHeader, "span 2, growx, wrap");
        courtCards.setOpaque(false);
        add(courtCards, "span 2, growx, h 88!");

        courtSelector.addActionListener(event -> selectCourt());
        refresh();
    }

    void refresh() {
        refreshing = true;
        List<CourtView> courts = management.courts();
        courtSelector.removeAllItems();
        courts.forEach(court -> courtSelector.addItem(court.getNumber()));
        courtSelector.setSelectedItem(courtPanel.selectedCourtNumber());
        refreshing = false;

        CourtView selected = management.court(courtPanel.selectedCourtNumber());
        courtName.setText("Quadra " + selected.getNumber());
        courtDetails.setText(selected.getCameras().size() + " câmeras instaladas");
        recording.setText("●  " + selected.getSecondsRecorded() + " segundos no buffer");
        quickPtz.refresh();
        rebuildCourtCards(courts);
    }

    private JPanel hero(JButton save) {
        JPanel hero = new JPanel(new MigLayout("insets 28, fill", "[grow][430!]", "push[][]12[]push"));
        hero.setBackground(VarZinhoTheme.SIDEBAR);
        hero.putClientProperty("FlatLaf.style", "arc: 20; background: $VarZinho.sidebarBackground");
        JLabel status = new JLabel("●  CAPTURA CONTÍNUA");
        status.setForeground(VarZinhoTheme.ACCENT);
        VarZinhoTheme.styleEyebrow(status);
        courtName.setForeground(Color.WHITE);
        courtDetails.setForeground(VarZinhoTheme.SIDEBAR_MUTED);
        recording.setForeground(VarZinhoTheme.ACCENT);
        courtName.setFont(courtName.getFont().deriveFont(java.awt.Font.BOLD, 26f));
        hero.add(status, "cell 0 0");
        hero.add(courtName, "cell 0 1");
        hero.add(courtDetails, "cell 0 2");
        hero.add(recording, "cell 0 3");
        save.setText("Salvar melhor momento");
        hero.add(save, "cell 1 1, span 1 2, grow, h 82!");
        return hero;
    }

    private void rebuildCourtCards(List<CourtView> courts) {
        courtCards.removeAll();
        for (CourtView court : courts) {
            JButton card = new JButton("<html><b>Quadra " + court.getNumber() + "</b><br>"
                    + court.getCameras().size() + " câmeras · " + court.getSecondsRecorded()
                    + " s no buffer</html>", VarZinhoIcons.court(VarZinhoTheme.EMERALD));
            card.setHorizontalAlignment(JButton.LEFT);
            card.setIconTextGap(14);
            card.putClientProperty("FlatLaf.style", "background: $VarZinho.cardBackground; arc: 16; borderWidth: 1");
            card.addActionListener(event -> {
                courtPanel.selectCourt(court.getNumber());
                refresh();
            });
            courtCards.add(card);
        }
        courtCards.revalidate();
        courtCards.repaint();
    }

    private void selectCourt() {
        if (refreshing || courtSelector.getSelectedItem() == null) return;
        courtPanel.selectCourt((Integer) courtSelector.getSelectedItem());
        refresh();
    }
}
