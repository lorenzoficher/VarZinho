package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.ManageGym;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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
    private final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel bufferStatus = new JLabel();
    private boolean phoneDropReported;

    /**
     * @param gymName shown in the title and the header
     * @param bufferSeconds how long each camera's buffer is, for the status line and for
     *     the cameras installed from the window
     */
    public VarZinhoWindow(String gymName, ManageGym management, CaptureHighlight captureHighlight,
                          BrowseArchive archive, Feed feed, int bufferSeconds) {
        super("VarZinho - Highlight Capture");
        if (gymName == null || gymName.isBlank()) throw new IllegalArgumentException("a window needs the gym's name");
        if (management == null) throw new IllegalArgumentException("a window needs the gym's management");
        if (captureHighlight == null) throw new IllegalArgumentException("a window needs a way to capture");
        if (archive == null) throw new IllegalArgumentException("a window needs the archive to browse");
        if (feed == null) throw new IllegalArgumentException("a window needs a feed of footage");
        if (bufferSeconds <= 0) throw new IllegalArgumentException("a window needs a positive buffer length");
        this.management = management;
        this.bufferSeconds = bufferSeconds;
        clocks.add(new Timer(feed.periodMillis(), event -> advance(feed)));
        this.courtPanel = new CourtPanel(management, bufferSeconds, this::showError);
        this.archivePanel = new ArchivePanel(management, archive, ZoneId.systemDefault(), this::showError);
        this.capturePanel = new CapturePanel(courtPanel, captureHighlight, ZoneId.systemDefault(),
                this::showError, highlight -> archivePanel.refresh());
        courtPanel.onCourtAdded(archivePanel::refreshCourts);
        setTitle("VarZinho - " + gymName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout(8, 8));
        header.add(buildTitle(gymName));
        top.add(header, BorderLayout.NORTH);
        top.add(courtPanel, BorderLayout.CENTER);
        top.add(capturePanel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(archivePanel, BorderLayout.CENTER);
        showBufferStatus();
        add(bufferStatus, BorderLayout.SOUTH);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pack();
        setLocationRelativeTo(null);
        clocks.forEach(Timer::start);
    }

    private static JLabel buildTitle(String gymName) {
        JLabel title = new JLabel(gymName);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        return title;
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
        JButton liveView = new JButton("Open live view");
        liveView.addActionListener(event -> openLiveView(streamUrl));
        header.add(liveView);
        pack();
    }

    private void openLiveView(String streamUrl) {
        try {
            Desktop.getDesktop().browse(URI.create(streamUrl));
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException failure) {
            showError("The live view could not be opened: " + failure.getMessage());
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
        showError("The phone stopped streaming, so its court can no longer capture. "
                + "Restart VarZinho to reconnect.");
    }

    private void advance(Feed feed) {
        try {
            feed.tick(Instant.now());
            showBufferStatus();
        } catch (IOException failure) {
            bufferStatus.setText("The phone's footage could not be read: " + failure.getMessage());
        }
    }

    private void showBufferStatus() {
        CourtView court = management.court(courtPanel.selectedCourtNumber());
        bufferStatus.setText("Court " + court.getNumber() + " buffer: "
                + court.getSecondsRecorded() + " of " + bufferSeconds + " seconds.");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }
}
