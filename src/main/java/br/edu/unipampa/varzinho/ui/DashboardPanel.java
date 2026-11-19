package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.function.Consumer;

/** Live operational summary with shortcuts to each real area of the application. */
final class DashboardPanel extends JPanel {
    private final ManageGym management;
    private final CourtPanel courtPanel;
    private final JLabel courts = new JLabel();
    private final JLabel cameras = new JLabel();
    private final JLabel selectedCourt = new JLabel();

    DashboardPanel(ManageGym management, CourtPanel courtPanel, Consumer<SidebarPanel.Page> navigate) {
        super(new MigLayout("insets 0, fill, wrap 3", "[grow,fill][grow,fill][grow,fill]", "[]16[]24[]"));
        if (management == null) throw new IllegalArgumentException("a dashboard needs gym management");
        if (courtPanel == null) throw new IllegalArgumentException("a dashboard needs the court selection");
        if (navigate == null) throw new IllegalArgumentException("a dashboard needs navigation");
        this.management = management;
        this.courtPanel = courtPanel;

        add(metric("Quadras cadastradas", courts), "grow");
        add(metric("Câmeras instaladas", cameras), "grow");
        add(metric("Quadra selecionada", selectedCourt), "grow, wrap");

        JPanel action = new JPanel(new MigLayout("insets 24, fillx, wrap 2", "[grow,fill][grow,fill]", "[]16[][]"));
        VarZinhoTheme.styleCard(action);
        JLabel title = new JLabel("Central de operações");
        VarZinhoTheme.styleTitle(title);
        action.add(title, "span 2, wrap");
        action.add(shortcut("Gerenciar quadras", VarZinhoIcons.courts(), SidebarPanel.Page.COURTS, navigate), "h 48!");
        action.add(shortcut("Controlar câmeras", VarZinhoIcons.record(), SidebarPanel.Page.CAMERAS, navigate), "h 48!");
        action.add(shortcut("Salvar melhor momento", VarZinhoIcons.aim(), SidebarPanel.Page.HIGHLIGHTS, navigate), "h 48!");
        action.add(shortcut("Consultar arquivos", VarZinhoIcons.archive(), SidebarPanel.Page.ARCHIVE, navigate), "h 48!");
        add(action, "span 3, growx");
        refresh();
    }

    void refresh() {
        List<CourtView> shownCourts = management.courts();
        int cameraCount = shownCourts.stream().mapToInt(court -> court.getCameras().size()).sum();
        courts.setText(Integer.toString(shownCourts.size()));
        cameras.setText(Integer.toString(cameraCount));
        selectedCourt.setText("Quadra " + courtPanel.selectedCourtNumber());
    }

    private static JPanel metric(String caption, JLabel value) {
        JPanel card = new JPanel(new MigLayout("insets 20, wrap 1", "[grow]", "[][]"));
        VarZinhoTheme.styleCard(card);
        JLabel label = new JLabel(caption);
        VarZinhoTheme.styleEyebrow(label);
        VarZinhoTheme.styleTitle(value);
        card.add(label);
        card.add(value);
        return card;
    }

    private static JButton shortcut(String text, javax.swing.Icon icon, SidebarPanel.Page page,
                                    Consumer<SidebarPanel.Page> navigate) {
        JButton button = new JButton(text, icon);
        button.addActionListener(event -> navigate.accept(page));
        return button;
    }
}
