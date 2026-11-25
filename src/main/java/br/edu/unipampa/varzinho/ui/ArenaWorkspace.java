package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;

import java.awt.Dimension;
import java.time.ZoneId;

/** The complete desktop workspace, also renderable without opening a window. */
final class ArenaWorkspace extends JPanel {
    private final ManageGym management;
    private final int bufferSeconds;
    private final CourtPanel courtPanel;
    private final ArchivePanel archivePanel;
    private final DashboardPanel dashboard;
    private final JPanel header = new JPanel(new MigLayout("insets 7 22, fillx, gapy 1", "[grow][]", "[][]"));
    private final JLabel pageTitle = buildTitle("Visão geral da arena");
    private final JLabel bufferStatus = new JLabel();
    private final SidebarPanel sidebar;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final JPanel cameraPage;
    private final JPanel highlightsPage = new JPanel(new BorderLayout(12, 12));
    private final JPanel filesPage = new JPanel(new BorderLayout());

    ArenaWorkspace(String gymName, SignedInOperator session, ManageGym management, CaptureHighlight captureHighlight,
                   BrowseArchive archive, int bufferSeconds, CaptureActivity captureActivity, MessageBar messages) {
        super(new BorderLayout());
        if (gymName == null || gymName.isBlank()) throw new IllegalArgumentException("a window needs the gym's name");
        if (session == null) throw new IllegalArgumentException("a window needs the signed-in operator");
        if (management == null) throw new IllegalArgumentException("a window needs the gym's management");
        if (captureHighlight == null) throw new IllegalArgumentException("a window needs a way to capture");
        if (archive == null) throw new IllegalArgumentException("a window needs the archive to browse");
        if (bufferSeconds <= 0) throw new IllegalArgumentException("a window needs a positive buffer length");
        var operator = session.getOperator();
        this.management = management;
        this.bufferSeconds = bufferSeconds;
        this.courtPanel = new CourtPanel(management, bufferSeconds, messages::show, captureActivity);
        this.archivePanel = new ArchivePanel(management::courts, archive, ZoneId.systemDefault(), messages::show);
        CourtSummaryPanel summary = new CourtSummaryPanel(courtPanel, management, archive, ZoneId.systemDefault(), messages::show);
        CapturePanel capturePanel = new CapturePanel(courtPanel, captureHighlight, ZoneId.systemDefault(),
                messages::show, highlight -> {
                    archivePanel.captureSaved();
                    summary.refresh();
                    if (ArchivePanel.playable(highlight.getClipPath())) {
                        messages.show("Vídeo salvo na quadra " + highlight.getCourtNumber() + ".");
                        messages.saved();
                    } else {
                        messages.show("Lance registrado sem MP4. Para gravar vídeo, use a quadra do celular conectado.");
                    }
                }, captureActivity);
        JLabel eyebrow = new JLabel("CENTRAL DE OPERAÇÕES");
        VarZinhoTheme.styleEyebrow(eyebrow);
        header.add(eyebrow, "cell 0 0");
        header.add(pageTitle, "cell 0 1, growx");

        capturePanel.bindShortcut(this);
        JPanel courtsPage = new JPanel(new BorderLayout(0, 8));
        courtsPage.add(summary, BorderLayout.NORTH);
        courtsPage.add(courtPanel.courtPage(), BorderLayout.CENTER);
        pages.add(courtsPage, SidebarPanel.Page.COURTS.name());
        pages.add(ScrollablePage.around(new OperatorsPanel(session, messages::show)), SidebarPanel.Page.OPERATORS.name());
        cameraPage = courtPanel.cameraPage();
        pages.add(ScrollablePage.around(cameraPage), SidebarPanel.Page.CAMERAS.name());
        highlightsPage.add(capturePanel, BorderLayout.NORTH);
        highlightsPage.add(bufferStatus, BorderLayout.SOUTH);
        pages.add(highlightsPage, SidebarPanel.Page.HIGHLIGHTS.name());
        pages.add(filesPage, SidebarPanel.Page.ARCHIVE.name());
        pages.add(ScrollablePage.around(new SettingsPanel(courtPanel, messages::show)), SidebarPanel.Page.SETTINGS.name());

        sidebar = new SidebarPanel(gymName, operator, this::navigate);
        dashboard = new DashboardPanel(management, courtPanel, capturePanel, this::navigate, messages::show);
        courtPanel.devices().onRefresh(() -> { dashboard.refresh(); refreshStatus(); });
        courtPanel.devices().onPosition(() -> {
            navigate(SidebarPanel.Page.CAMERAS);
            courtPanel.moveTargetSelector().requestFocusInWindow();
            courtPanel.moveTargetSelector().scrollRectToVisible(
                    new java.awt.Rectangle(courtPanel.moveTargetSelector().getSize()));
        });
        JPanel context = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        context.setOpaque(false);
        JLabel selection = new JLabel("Quadra selecionada");
        selection.setLabelFor(dashboard.selector());
        context.add(selection);
        dashboard.selector().setPreferredSize(new java.awt.Dimension(162, 38));
        context.add(dashboard.selector());
        JButton notices = new JButton(VarZinhoIcons.arena("bell", 20, VarZinhoTheme.EMERALD));
        notices.setToolTipText("Avisos da arena");
        notices.getAccessibleContext().setAccessibleName("Avisos da arena");
        notices.addActionListener(e -> {
            javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
            popup.add(new JLabel(messages.message()));
            popup.show(notices, 0, notices.getHeight());
        });
        context.add(notices);
        JLabel profile = new JLabel(SidebarPanel.initials(operator.getName()), JLabel.CENTER);
        profile.setPreferredSize(new java.awt.Dimension(40, 40));
        profile.setBackground(VarZinhoTheme.ACCENT);
        profile.setForeground(VarZinhoTheme.ON_ACCENT);
        profile.setOpaque(true);
        profile.setToolTipText(operator.getName() + " · @" + operator.getUsername());
        context.add(profile);
        header.add(context, "cell 1 0, spany 2");
        pages.add(ScrollablePage.around(dashboard), SidebarPanel.Page.DASHBOARD.name());
        courtPanel.onCourtAdded(() -> {
            archivePanel.refreshCourts();
            dashboard.refresh();
        });
        // The archive filter keeps a removed court: its highlights stay in the archive.
        courtPanel.onCourtRemoved(() -> {
            dashboard.refresh();
            refreshStatus();
        });
        courtPanel.onCourtChange(() -> {
            dashboard.refresh();
            refreshStatus();
        });
        courtPanel.onIdentityChange(() -> { dashboard.refresh(); refreshStatus(); });
        archivePanel.onCount(sidebar::showArchiveCount);
        navigate(SidebarPanel.Page.DASHBOARD);
        add(sidebar, BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout());
        header.putClientProperty("FlatLaf.style", "background: $VarZinho.cardBackground");
        content.add(header, BorderLayout.NORTH);
        pages.setBorder(BorderFactory.createEmptyBorder(16, 22, 12, 22));
        content.add(pages, BorderLayout.CENTER);
        content.add(messages, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        refreshStatus();
    }

    void navigate(SidebarPanel.Page page) {
        pageLayout.show(pages, page.name());
        pageTitle.setText(titleOf(page));
        if (page == SidebarPanel.Page.COURTS) courtPanel.refreshCourtOverview();
        sidebar.select(page);
        if (page == SidebarPanel.Page.DASHBOARD) {
            dashboard.attach(courtPanel.devices(), archivePanel);
            dashboard.refresh();
            archivePanel.refresh();
        }
        if (page == SidebarPanel.Page.CAMERAS) cameraPage.add(courtPanel.devices(), BorderLayout.CENTER);
        if (page == SidebarPanel.Page.HIGHLIGHTS) {
            highlightsPage.add(archivePanel, BorderLayout.CENTER);
            archivePanel.refresh();
        }
        if (page == SidebarPanel.Page.ARCHIVE) {
            filesPage.add(archivePanel);
            archivePanel.refresh();
        }
        pages.revalidate();
        pages.repaint();
    }

    void selectCourt(int number) { courtPanel.selectCourt(number); }
    void enableRecordingEditing(br.edu.unipampa.varzinho.application.port.RecordingEditor editor) {
        archivePanel.enableEditing(editor);
    }
    void refreshDevices() { courtPanel.devices().refresh(); }

    void liveActions(Runnable open, Runnable copy, String hint) {
        dashboard.liveActions(open, copy, hint);
    }

    private static String titleOf(SidebarPanel.Page page) {
        switch (page) {
            case DASHBOARD: return "Visão geral da arena";
            case COURTS: return "Gerenciamento de quadras";
            case CAMERAS: return "Controle de câmeras";
            case HIGHLIGHTS: return "Melhores momentos";
            case ARCHIVE: return "Arquivos de destaques";
            case OPERATORS: return "Operadores";
            case SETTINGS: return "Configurações";
            default: throw new IllegalArgumentException("unknown page " + page);
        }
    }

    private static JLabel buildTitle(String gymName) {
        JLabel title = new JLabel(gymName);
        VarZinhoTheme.styleTitle(title);
        return title;
    }

    void refreshStatus() {
        CourtView court = management.court(courtPanel.selectedCourtNumber());
        bufferStatus.setText("Buffer da quadra " + court.getNumber() + ": "
                + court.getSecondsRecorded() + " de " + bufferSeconds + " segundos.");
        sidebar.updateStatus(management.courts());
        dashboard.tick(court);
    }
}
