package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

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
 * the feeds it is handed. It builds none of what it shows — {@link Launcher} does.
 */
final class VarZinhoWindow extends JFrame {
    static final int BUFFER_SECONDS = 30;

    private final Gym gym;
    private final List<Timer> clocks = new ArrayList<>();
    private final CourtPanel courtPanel;
    private final ArchivePanel archivePanel;
    private final CapturePanel capturePanel;
    private final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel bufferStatus = new JLabel();
    private boolean phoneDropReported;

    VarZinhoWindow(GymSession session, HighlightRepository repository, Feed feed, PendingClipWrites pendingWrites) {
        super("VarZinho - Highlight Capture");
        if (session == null) throw new IllegalArgumentException("a window needs a gym session");
        if (repository == null) throw new IllegalArgumentException("a window needs a highlight repository");
        if (feed == null) throw new IllegalArgumentException("a window needs a feed of footage");
        this.gym = session.gym();
        clocks.add(new Timer(feed.periodMillis(), event -> advance(feed)));
        this.courtPanel = new CourtPanel(gym, BUFFER_SECONDS, this::showError);
        this.archivePanel = new ArchivePanel(gym, repository, ZoneId.systemDefault(), this::showError);
        this.capturePanel = new CapturePanel(courtPanel, repository, ZoneId.systemDefault(), pendingWrites,
                this::showError, highlight -> archivePanel.refresh());
        courtPanel.onCourtAdded(archivePanel::refreshCourts);
        courtPanel.onGymChanged(() -> session.save(this::showError));
        setTitle("VarZinho - " + gym.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout(8, 8));
        header.add(buildTitle());
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

    private JLabel buildTitle() {
        JLabel title = new JLabel(gym.getName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        return title;
    }

    /**
     * Runs the phone's feed on a clock of its own, shows its court and offers the stream
     * in the browser.
     */
    void followPhone(Camera phone, Feed phoneFeed, int phoneCourt, String streamUrl) {
        if (phone == null) throw new IllegalArgumentException("a followed phone needs its camera");
        if (phoneFeed == null) throw new IllegalArgumentException("a followed phone needs its feed");
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

    // From here on the domain answers by itself: a capture on the phone's court finds no
    // recording camera and says so. FFmpeg exiting and the phone going silent are two
    // signs of the same drop, and the operator hears of it once.
    void phoneDropped(Camera phone) {
        if (phoneDropReported) {
            return;
        }
        phoneDropReported = true;
        if (phone.isRecording()) {
            phone.stopRecording();
        }
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
        Court court = courtPanel.selectedCourt();
        bufferStatus.setText("Court " + court.getNumber() + " buffer: "
                + court.secondsRecorded() + " of " + BUFFER_SECONDS + " seconds.");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }
}
