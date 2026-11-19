package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.OperatorView;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The operator's window: the courts, the capture button and the archive, kept moving by
 * the feeds it is handed. It reaches the gym and the archive only through the use cases
 * it is given, and never holds a domain object. It builds none of what it shows —
 * {@link br.edu.unipampa.varzinho.bootstrap.Launcher} does.
 */
public final class VarZinhoWindow extends JFrame {
    private final ManageGym management;
    private final int bufferSeconds;
    private final List<Timer> clocks = new ArrayList<>();
    private final CourtPanel courtPanel;
    private final ArchivePanel archivePanel;
    private final CapturePanel capturePanel;
    private DashboardPanel dashboard;
    private final JPanel header = new JPanel(new MigLayout("insets 4 2 14 2, fillx", "[grow][]", "[][]"));
    private final JLabel pageTitle = buildTitle("Visão geral da arena");
    private final MessageBar messages = new MessageBar();
    private final JLabel bufferStatus = new JLabel();
    private SidebarPanel sidebar;
    private boolean phoneDropReported;

    /**
     * @param gymName shown in the title and the header
     * @param bufferSeconds how long each camera's buffer is, for the status line and for
     *     the cameras installed from the window
     */
    public VarZinhoWindow(String gymName, OperatorView operator, ManageGym management, CaptureHighlight captureHighlight,
                          BrowseArchive archive, Feed feed, int bufferSeconds) {
        super("VarZinho Arena");
        if (gymName == null || gymName.isBlank()) throw new IllegalArgumentException("a window needs the gym's name");
        if (management == null) throw new IllegalArgumentException("a window needs the gym's management");
        if (captureHighlight == null) throw new IllegalArgumentException("a window needs a way to capture");
        if (archive == null) throw new IllegalArgumentException("a window needs the archive to browse");
        if (feed == null) throw new IllegalArgumentException("a window needs a feed of footage");
        if (bufferSeconds <= 0) throw new IllegalArgumentException("a window needs a positive buffer length");
        this.management = management;
        this.bufferSeconds = bufferSeconds;
        clocks.add(new Timer(feed.periodMillis(), event -> advance(feed)));
        if (operator == null) throw new IllegalArgumentException("a window needs the signed-in operator");
        this.courtPanel = new CourtPanel(management, bufferSeconds, messages::show);
        this.archivePanel = new ArchivePanel(management::courts, archive, ZoneId.systemDefault(), messages::show);
        this.capturePanel = new CapturePanel(courtPanel, captureHighlight, ZoneId.systemDefault(),
                messages::show, highlight -> {
                    archivePanel.refresh();
                    messages.saved();
                });
        setTitle("VarZinho Arena");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        CardLayout pageLayout = new CardLayout();
        JPanel pages = new JPanel(pageLayout);
        JLabel eyebrow = new JLabel("CENTRAL DE OPERAÇÕES");
        VarZinhoTheme.styleEyebrow(eyebrow);
        header.add(eyebrow, "cell 0 0");
        header.add(pageTitle, "cell 0 1");

        pages.add(courtPanel.courtPage(), SidebarPanel.Page.COURTS.name());
        JPanel cameraPage = courtPanel.cameraPage();
        pages.add(cameraPage, SidebarPanel.Page.CAMERAS.name());
        JPanel highlightsPage = new JPanel(new BorderLayout(12, 12));
        highlightsPage.add(capturePanel, BorderLayout.NORTH);
        highlightsPage.add(bufferStatus, BorderLayout.CENTER);
        pages.add(highlightsPage, SidebarPanel.Page.HIGHLIGHTS.name());
        JPanel filesPage = new JPanel(new BorderLayout());
        pages.add(filesPage, SidebarPanel.Page.ARCHIVE.name());
        pages.add(new SettingsPanel(), SidebarPanel.Page.SETTINGS.name());

        SidebarPanel[] sidebarHolder = new SidebarPanel[1];
        DashboardPanel[] dashboardHolder = new DashboardPanel[1];
        Consumer<SidebarPanel.Page> navigate = page -> {
            pageLayout.show(pages, page.name());
            pageTitle.setText(titleOf(page));
            if (sidebarHolder[0] != null) sidebarHolder[0].select(page);
            if (page == SidebarPanel.Page.DASHBOARD) {
                dashboardHolder[0].attach(courtPanel.devices(), archivePanel);
                dashboardHolder[0].refresh();
                archivePanel.refresh();
            }
            if (page == SidebarPanel.Page.CAMERAS) cameraPage.add(courtPanel.devices(), BorderLayout.CENTER);
            if (page == SidebarPanel.Page.ARCHIVE) {
                filesPage.add(archivePanel);
                archivePanel.refresh();
            }
            pages.revalidate();
            pages.repaint();
        };
        dashboard = new DashboardPanel(
                management, courtPanel, capturePanel, navigate, messages::show);
        dashboardHolder[0] = dashboard;
        courtPanel.devices().onRefresh(dashboard::refresh);
        courtPanel.devices().onPosition(() -> navigate.accept(SidebarPanel.Page.CAMERAS));
        JPanel context = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 2));
        context.setOpaque(false);
        JLabel selection = new JLabel("Quadra selecionada");
        selection.setLabelFor(dashboard.selector());
        context.add(selection);
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
        JLabel profile = new JLabel(operator.getName());
        profile.setToolTipText("@" + operator.getUsername());
        context.add(profile);
        header.add(context, "cell 1 0, spany 2");
        JScrollPane dashboardScroll = new JScrollPane(dashboard,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dashboardScroll.setBorder(BorderFactory.createEmptyBorder());
        dashboardScroll.getVerticalScrollBar().setUnitIncrement(20);
        pages.add(dashboardScroll, SidebarPanel.Page.DASHBOARD.name());
        courtPanel.onCourtAdded(() -> {
            archivePanel.refreshCourts();
            dashboard.refresh();
        });
        courtPanel.onCourtChange(() -> {
            dashboard.refresh();
            showBufferStatus();
        });
        sidebar = new SidebarPanel(gymName, operator, navigate);
        sidebarHolder[0] = sidebar;
        archivePanel.onCount(sidebar::showArchiveCount);
        navigate.accept(SidebarPanel.Page.DASHBOARD);
        add(sidebar, BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        content.add(header, BorderLayout.NORTH);
        content.add(pages, BorderLayout.CENTER);
        content.add(messages, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        java.awt.Rectangle bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        setPreferredSize(new Dimension(Math.min(1460, bounds.width), Math.min(900, bounds.height)));
        showBufferStatus();
        pack();
        setLocationRelativeTo(null);
        clocks.forEach(Timer::start);
    }

    private static String titleOf(SidebarPanel.Page page) {
        switch (page) {
            case DASHBOARD: return "Visão geral da arena";
            case COURTS: return "Gerenciamento de quadras";
            case CAMERAS: return "Controle de câmeras";
            case HIGHLIGHTS: return "Melhores momentos";
            case ARCHIVE: return "Arquivos de destaques";
            case SETTINGS: return "Configurações";
            default: throw new IllegalArgumentException("unknown page " + page);
        }
    }

    private static JLabel buildTitle(String gymName) {
        JLabel title = new JLabel(gymName);
        VarZinhoTheme.styleTitle(title);
        return title;
    }

    static JButton buildThemeButton() {
        JButton button = new JButton(VarZinhoIcons.theme());
        button.setText("Alternar tema");
        button.setToolTipText("Alternar entre os temas claro e escuro");
        button.addActionListener(event -> VarZinhoTheme.toggle());
        return button;
    }

    /**
     * Runs the phone's feed on a clock of its own, shows its court and offers the stream
     * in the browser.
     */
    public void followPhone(Feed phoneFeed, int phoneCourt, String streamUrl) {
        if (phoneFeed == null) throw new IllegalArgumentException("a followed phone needs its feed");
        if (streamUrl == null || streamUrl.isBlank()) {
            throw new IllegalArgumentException("a followed phone needs its stream address");
        }
        Timer clock = new Timer(phoneFeed.periodMillis(), event -> advance(phoneFeed));
        clocks.add(clock);
        clock.start();
        courtPanel.selectCourt(phoneCourt);
        offerLiveView(streamUrl);
    }

    // Watching the phone in the browser is the whole preview: showing video inside
    // Swing would need a decoding library, which this project does not take on.
    private void offerLiveView(String streamUrl) {
        JButton liveView = new JButton("Abrir transmissão ao vivo");
        liveView.setIcon(VarZinhoIcons.openClip());
        liveView.addActionListener(event -> openLiveView(streamUrl));
        header.add(liveView, "cell 2 0, spany 2");
        pack();
    }

    private void openLiveView(String streamUrl) {
        try {
            Desktop.getDesktop().browse(URI.create(streamUrl));
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException failure) {
            messages.show("Não foi possível abrir a transmissão ao vivo: " + failure.getMessage());
        }
    }

    /**
     * Tells the operator, once, that the phone's court can no longer capture. FFmpeg
     * exiting and the phone going silent are two signs of the same drop.
     */
    public void phoneDropped() {
        if (phoneDropReported) {
            return;
        }
        phoneDropReported = true;
        showBufferStatus();
        messages.show("O celular parou de transmitir e a quadra não pode mais capturar. "
                + "Reinicie o VarZinho para reconectar.");
    }

    private void advance(Feed feed) {
        try {
            feed.tick(Instant.now());
            showBufferStatus();
        } catch (IOException failure) {
            messages.show("Não foi possível ler as imagens do celular: " + failure.getMessage());
        }
    }

    private void showBufferStatus() {
        CourtView court = management.court(courtPanel.selectedCourtNumber());
        bufferStatus.setText("Buffer da quadra " + court.getNumber() + ": "
                + court.getSecondsRecorded() + " de " + bufferSeconds + " segundos.");
        if (sidebar != null) sidebar.updateStatus(management.courts());
        if (dashboard != null) dashboard.tick(court);
        courtPanel.refreshCourtOverview();
    }
}
