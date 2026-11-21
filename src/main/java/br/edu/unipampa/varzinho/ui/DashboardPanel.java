package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.ManageGym;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

/** Live arena dashboard composed only from data and actions the application owns. */
final class DashboardPanel extends JPanel {
    private final ManageGym management;
    private final CourtPanel courtPanel;
    private final QuickPtzPanel quickPtz;
    private final JComboBox<Integer> courtSelector = new JComboBox<>();
    private final JButton save;
    private final JButton liveView = new JButton("Abrir transmissão ao vivo", VarZinhoIcons.openClip());
    private final JButton share = new JButton("Copiar link público");
    private final JLabel courtName = new JLabel();
    private final JLabel courtDetails = new JLabel();
    private final JLabel status = new JLabel();
    private final JLabel recording = new JLabel();
    private final JProgressBar buffer = new JProgressBar();
    private final CourtOverviewPanel overview;
    private boolean refreshing;
    private final java.util.Map<String, javax.swing.JToggleButton> sportButtons = new java.util.LinkedHashMap<>();
    private final javax.swing.ButtonGroup modalities = new javax.swing.ButtonGroup();
    private final JPanel devicesHost = new JPanel(new BorderLayout());
    private final JPanel archiveHost = new JPanel(new BorderLayout());

    DashboardPanel(ManageGym management, CourtPanel courtPanel, CapturePanel capturePanel,
                   Consumer<SidebarPanel.Page> navigate, Consumer<String> messages) {
        super(new MigLayout("insets 0, fillx, wrap 1", "[0:pref,grow,fill]", "[]14[]18[]14[]14[]"));
        if (management == null) throw new IllegalArgumentException("a dashboard needs gym management");
        if (courtPanel == null) throw new IllegalArgumentException("a dashboard needs the court selection");
        if (capturePanel == null) throw new IllegalArgumentException("a dashboard needs highlight capture");
        if (navigate == null) throw new IllegalArgumentException("a dashboard needs navigation");
        if (messages == null) throw new IllegalArgumentException("a dashboard needs message feedback");
        this.management = management;
        this.courtPanel = courtPanel;
        this.quickPtz = new QuickPtzPanel(management, courtPanel::selectedCourtNumber, messages,
                courtPanel.devices()::refresh);

        JPanel strip = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        strip.setOpaque(false);
        strip.add(new JLabel("Modalidade"));
        for (String sport : List.of("Futebol", "Vôlei", "Basquete", "Beach Tennis")) {
            javax.swing.JToggleButton button = new javax.swing.JToggleButton(sport);
            button.setIcon(sportIcon(sport, VarZinhoTheme.EMERALD));
            button.setSelectedIcon(sportIcon(sport, VarZinhoTheme.ACCENT));
            button.putClientProperty("FlatLaf.style", "arc: 14; margin: 7,12,7,12; selectedBackground: $VarZinho.sidebarBackground; selectedForeground: $VarZinho.sidebarForeground");
            sportButtons.put(sport, button);
            modalities.add(button);
            strip.add(button);
            button.setToolTipText("Definir a modalidade desta quadra nesta sessão");
            button.addActionListener(e -> {
                courtPanel.chooseSport(sport);
            });
        }
        add(strip);

        save = capturePanel.additionalSaveButton();
        liveView.setEnabled(false);
        share.setEnabled(false);
        share.setToolTipText("Disponível quando o endereço HTTPS dos espectadores estiver configurado.");
        AdaptivePair live = new AdaptivePair(850, "[0:pref,grow,fill][300:340:390,fill]");
        live.add(hero(save), "h 214!");
        live.add(quickPtz, "h 214!");
        add(live);

        overview = new CourtOverviewPanel(courtPanel::selectCourt, courtPanel::sportOf, courtPanel::nameOf,
                () -> navigate.accept(SidebarPanel.Page.COURTS));
        add(overview, "gaptop 6");
        add(devicesHost, "gaptop 16");
        add(archiveHost, "gaptop 16");

        courtSelector.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focused) {
                Object label = value instanceof Integer number ? courtPanel.nameOf(number) : value;
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });
        courtSelector.addActionListener(event -> selectCourt());
        refresh();
    }

    JButton saveButton() { return save; }

    void liveActions(Runnable open, Runnable copy, String hint) {
        for (var listener : liveView.getActionListeners()) liveView.removeActionListener(listener);
        for (var listener : share.getActionListeners()) share.removeActionListener(listener);
        liveView.setEnabled(open != null);
        liveView.setToolTipText(hint);
        share.setEnabled(copy != null);
        share.setToolTipText(copy == null ? "Configure o endereço HTTPS público da arena para compartilhar."
                : "Copiar o endereço da partida para enviar aos espectadores");
        if (open != null) liveView.addActionListener(e -> open.run());
        if (copy != null) share.addActionListener(e -> copy.run());
    }

    JComboBox<Integer> selector() { return courtSelector; }

    void attach(CameraListPanel devices, ArchivePanel history) {
        devicesHost.add(devices);
        archiveHost.add(history);
        revalidate();
    }

    void tick(CourtView selected) {
        int required = selected.getCaptureSeconds();
        int recorded = Math.min(required, selected.getSecondsRecorded());
        buffer.setMaximum(required);
        buffer.setValue(recorded);
        recording.setText(recorded + " de " + required + " segundos no buffer");
        int live = selected.getRecordingCameraCount();
        String sport = courtPanel.sportOf(selected.getNumber());
        var capturingCamera = selected.getCameras().stream().filter(CameraView::isRecording).findFirst();
        boolean capturesVideo = capturingCamera.filter(camera -> camera.getKind() == CameraKind.STREAM).isPresent();
        String source = capturingCamera.map(camera -> camera.getKind() == CameraKind.STREAM
                        ? camera.getId() + " · grava vídeo MP4" : "Simulação · sem vídeo MP4").orElse("Sem câmera ativa");
        courtDetails.setText((sport == null ? "" : sport + " · ") + source);
        if (live == 0) status.setText("Gravação parada");
        else if (recorded < required) status.setText("Preenchendo o buffer");
        else status.setText(capturesVideo ? "Pronto para salvar vídeo" : "Buffer de simulação cheio");
    }

    void refresh() {
        refreshing = true;
        List<CourtView> courts = management.courts();
        courtSelector.removeAllItems();
        courts.forEach(court -> courtSelector.addItem(court.getNumber()));
        courtSelector.setSelectedItem(courtPanel.selectedCourtNumber());
        refreshing = false;

        CourtView selected = management.court(courtPanel.selectedCourtNumber());
        courtName.setText(courtPanel.nameOf(selected.getNumber()));
        String sport = courtPanel.sportOf(selected.getNumber());
        modalities.clearSelection();
        if (sport != null) sportButtons.get(sport).setSelected(true);
        tick(selected);
        quickPtz.refresh();
        overview.refresh(courts, selected.getNumber());
    }

    private JPanel hero(JButton save) {
        JPanel hero = new PitchPanel(new MigLayout("insets 18 24, fill", "[grow,fill]10[pref,fill]", "push[]5[]4[]10[]4[]push"));
        hero.setBackground(VarZinhoTheme.SIDEBAR);
        VarZinhoTheme.styleEyebrow(status);
        status.putClientProperty("FlatLaf.style", "foreground: $VarZinho.accent");
        status.setFont(status.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        courtName.setForeground(Color.WHITE);
        courtDetails.setForeground(VarZinhoTheme.SIDEBAR_MUTED);
        recording.setForeground(Color.WHITE);
        buffer.setForeground(VarZinhoTheme.ACCENT);
        buffer.setBackground(VarZinhoTheme.SIDEBAR);
        buffer.getAccessibleContext().setAccessibleName("Preenchimento do buffer de captura");
        courtName.setFont(courtName.getFont().deriveFont(java.awt.Font.BOLD, 26f));
        hero.add(status, "cell 0 0");
        hero.add(courtName, "cell 0 1");
        hero.add(courtDetails, "cell 0 2");
        hero.add(recording, "cell 0 3");
        hero.add(buffer, "cell 0 4, w 80:170:200, h 6!");
        save.setText("Salvar Melhor Momento");
        save.setToolTipText("Salvar o trecho mais recente do buffer da quadra selecionada");
        save.setIcon(VarZinhoIcons.arena("replay", 40, VarZinhoTheme.EMERALD));
        save.setFont(save.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        save.setIconTextGap(12);
        JPanel actions = new JPanel(new MigLayout("insets 0, fillx, wrap 2", "[grow,fill][grow,fill]", "[]6[]"));
        actions.setOpaque(false);
        actions.add(save, "span 2, growx, h 64!");
        actions.add(liveView);
        actions.add(share);
        hero.add(actions, "cell 1 0, spany 5, aligny center");
        return hero;
    }

    private void selectCourt() {
        if (refreshing || courtSelector.getSelectedItem() == null) return;
        courtPanel.selectCourt((Integer) courtSelector.getSelectedItem());
    }

    static javax.swing.Icon sportIcon(String sport, Color color) {
        String icon = switch (sport) {
            case "Futebol" -> "football";
            case "Vôlei" -> "volleyball";
            case "Basquete" -> "basketball";
            case "Beach Tennis" -> "racket";
            default -> throw new IllegalArgumentException("unknown sport " + sport);
        };
        return VarZinhoIcons.arena(icon, 19, color);
    }
}
