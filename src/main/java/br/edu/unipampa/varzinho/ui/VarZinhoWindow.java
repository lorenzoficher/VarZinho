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
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The operator's window: the courts, the capture button and the archive, kept moving by
 * the feeds it is handed. It reaches the gym and the archive only through the use cases
 * it is given, and never holds a domain object. It builds none of what it shows —
 * {@link br.edu.unipampa.varzinho.bootstrap.Launcher} does.
 */
public final class VarZinhoWindow extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(VarZinhoWindow.class.getName());
    private final ManageGym management;
    private final int bufferSeconds;
    private final List<Timer> clocks = new ArrayList<>();
    private final CaptureActivity captureActivity = new CaptureActivity();
    private final CourtPanel courtPanel;
    private final ArchivePanel archivePanel;
    private final CapturePanel capturePanel;
    private final CourtSummaryPanel courtSummary;
    private final JPanel header = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]8[]"));
    private final MessageBar messages = new MessageBar();
    private final JLabel bufferStatus = new JLabel();
    private boolean phoneDropReported;

    /**
     * @param gymName shown in the title and the sidebar
     * @param bufferSeconds how long each camera's buffer is, for the status line and for
     *     the cameras installed from the window
     */
    public VarZinhoWindow(String gymName, OperatorView operator, ManageGym management, CaptureHighlight captureHighlight,
                          BrowseArchive archive, Feed feed, int bufferSeconds) {
        super("VarZinho Arena");
        if (gymName == null || gymName.isBlank()) throw new IllegalArgumentException("a window needs the gym's name");
        if (operator == null) throw new IllegalArgumentException("a window needs the signed-in operator");
        if (management == null) throw new IllegalArgumentException("a window needs the gym's management");
        if (captureHighlight == null) throw new IllegalArgumentException("a window needs a way to capture");
        if (archive == null) throw new IllegalArgumentException("a window needs the archive to browse");
        if (feed == null) throw new IllegalArgumentException("a window needs a feed of footage");
        if (bufferSeconds <= 0) throw new IllegalArgumentException("a window needs a positive buffer length");
        this.management = management;
        this.bufferSeconds = bufferSeconds;
        clocks.add(clockFor(feed, captureActivity, this::showBufferStatus, messages));
        this.courtPanel = new CourtPanel(management, bufferSeconds, messages::show);
        this.archivePanel = new ArchivePanel(management::courts, archive, ZoneId.systemDefault(), messages::show);
        this.courtSummary = new CourtSummaryPanel(courtPanel, management, archive, ZoneId.systemDefault(),
                messages::show);
        this.capturePanel = new CapturePanel(courtPanel, captureHighlight, ZoneId.systemDefault(),
                messages::show, highlight -> {
                    messages.show("Lance salvo na quadra " + highlight.getCourtNumber() + ".");
                    archivePanel.refresh();
                    courtSummary.refresh();
                }, captureActivity);
        courtPanel.onCourtAdded(archivePanel::refreshCourts);
        setTitle("VarZinho Arena — " + gymName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("Quadras");
        VarZinhoTheme.styleTitle(title);
        header.add(title, "cell 0 0");
        header.add(courtSummary, "cell 0 1, span 2, growx");
        top.add(header, BorderLayout.NORTH);
        top.add(courtPanel, BorderLayout.CENTER);
        top.add(capturePanel, BorderLayout.SOUTH);
        JPanel courtsPage = new JPanel(new BorderLayout(8, 8));
        courtsPage.add(top, BorderLayout.CENTER);
        courtsPage.add(bufferStatus, BorderLayout.SOUTH);
        setContentPane(navigation(gymName, operator, courtsPage, archivePanel, messages));
        showBufferStatus();
        pack();
        setLocationRelativeTo(null);
        clocks.forEach(Timer::start);
    }

    static JPanel navigation(String gymName, OperatorView operator, JPanel courtsPage, JPanel archivePage,
                             MessageBar messages) {
        CardLayout pageLayout = new CardLayout();
        JPanel pages = new JPanel(pageLayout);
        pages.add(courtsPage, SidebarPanel.Page.COURTS.name());
        pages.add(archivePage, SidebarPanel.Page.ARCHIVE.name());
        SidebarPanel sidebar = new SidebarPanel(gymName, operator,
                page -> pageLayout.show(pages, page.name()));
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(pages, BorderLayout.CENTER);
        content.add(messages, BorderLayout.SOUTH);
        JPanel navigation = new JPanel(new BorderLayout());
        navigation.add(sidebar, BorderLayout.WEST);
        navigation.add(content, BorderLayout.CENTER);
        return navigation;
    }

    public void showStartupWarnings(List<String> warnings) {
        messages.showStartupWarnings(warnings);
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
        Timer clock = clockFor(phoneFeed, captureActivity, this::showBufferStatus, messages);
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
        header.add(liveView, "cell 1 0");
        pack();
    }

    private void openLiveView(String streamUrl) {
        try {
            Desktop.getDesktop().browse(URI.create(streamUrl));
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException failure) {
            // The stream address can carry credentials, even when URI parsing fails.
            LOGGER.log(Level.WARNING, "Could not open live stream: {0}",
                    failure.toString().replace(streamUrl, "[configured stream]"));
            messages.show("Não foi possível abrir a transmissão ao vivo.");
        }
    }

    /**
     * Tells the operator, once, that the phone's court can no longer capture. FFmpeg
     * exiting and the phone going silent are two signs of the same drop.
     */
    public void phoneDropped(Runnable disconnect) {
        captureActivity.whenIdle(() -> {
            if (phoneDropReported) return;
            disconnect.run();
            phoneDropReported = true;
            courtPanel.refreshCameras();
            showBufferStatus();
            messages.show("O celular parou de transmitir. "
                    + "Reinicie o VarZinho para reconectar.");
        });
    }

    static Timer clockFor(Feed feed, CaptureActivity activity, Runnable refresh, MessageBar messages) {
        return new Timer(feed.periodMillis(), new ActionListener() {
            private boolean readFailed;

            @Override
            public void actionPerformed(ActionEvent event) {
                activity.ifIdle(this::tick);
            }

            private void tick() {
                try {
                    feed.tick(Instant.now());
                    readFailed = false;
                    refresh.run();
                } catch (IOException failure) {
                    if (readFailed) return;
                    readFailed = true;
                    LOGGER.log(Level.WARNING, "Could not read footage from the feed", failure);
                    messages.show("Não foi possível ler as imagens do celular.");
                }
            }
        });
    }

    private void showBufferStatus() {
        CourtView court = management.court(courtPanel.selectedCourtNumber());
        bufferStatus.setText("Buffer da quadra " + court.getNumber() + ": "
                + court.getSecondsRecorded() + " de " + bufferSeconds + " segundos.");
    }
}
