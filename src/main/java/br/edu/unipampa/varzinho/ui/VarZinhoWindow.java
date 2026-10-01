package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.repository.CsvGymRepository;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;
import br.edu.unipampa.varzinho.stream.FfmpegClipAssembler;
import br.edu.unipampa.varzinho.stream.FfmpegRecorder;
import br.edu.unipampa.varzinho.stream.SegmentIndex;
import br.edu.unipampa.varzinho.stream.SegmentLog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;

public final class VarZinhoWindow extends JFrame {
    private static final int BUFFER_SECONDS = 30;
    private static final String STREAM_URL_VARIABLE = "VARZINHO_STREAM_URL";
    private static final Path SEGMENT_FOLDER = Path.of("buffer");

    private final Gym gym;
    private final Feed feed;
    private final Timer clock;
    private final CourtPanel courtPanel;
    private final ArchivePanel archivePanel;
    private final CapturePanel capturePanel;
    private final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel bufferStatus = new JLabel();

    /**
     * Opens the window on the simulated cameras, or, when {@value #STREAM_URL_VARIABLE}
     * names a phone stream, on that phone as well.
     */
    public static void open() {
        SwingUtilities.invokeLater(() -> {
            HighlightRepository repository = new CsvHighlightRepository(
                    Path.of("data", "highlights.csv"));
            GymSession session = GymSession.start(new CsvGymRepository(Path.of("data", "gym.csv")),
                    VarZinhoWindow::showStartupError);
            String streamUrl = System.getenv(STREAM_URL_VARIABLE);
            if (streamUrl == null || streamUrl.isBlank()) {
                new VarZinhoWindow(session, repository, new LiveFeed(session.gym())).setVisible(true);
            } else {
                openWithPhone(session, repository, streamUrl);
            }
        });
    }

    /**
     * Records the phone into a court of its own, so a capture there always comes from
     * the phone: a court captures from its first recording camera, and on a shared court
     * that would be whichever camera was installed first.
     */
    private static void openWithPhone(GymSession session, HighlightRepository repository, String streamUrl) {
        FfmpegRecorder recorder = new FfmpegRecorder(streamUrl, SEGMENT_FOLDER);
        try {
            recorder.start();
        } catch (IOException cause) {
            JOptionPane.showMessageDialog(null, "The phone stream could not start (" + cause.getMessage()
                    + "). Opening with the simulated cameras only.", "Phone not connected",
                    JOptionPane.WARNING_MESSAGE);
            new VarZinhoWindow(session, repository, new LiveFeed(session.gym())).setVisible(true);
            return;
        }
        Gym gym = session.gym();
        SegmentIndex index = new SegmentIndex(FfmpegRecorder.RING_SEGMENTS);
        StreamCamera phone = new StreamCamera("phone-1", "Phone", Resolution.HD, BUFFER_SECONDS,
                streamUrl, new FfmpegClipAssembler(index));
        int phoneCourt = gym.getCourts().stream().mapToInt(Court::getNumber).max().orElse(0) + 1;
        gym.addCourt(new Court(phoneCourt));
        gym.installCamera(phoneCourt, phone);
        phone.startRecording();
        VarZinhoWindow window = new VarZinhoWindow(session, repository,
                new SegmentFeed(gym, new SegmentLog(recorder.segmentList()), index));
        window.courtPanel.selectCourt(phoneCourt);
        window.offerLiveView(streamUrl);
        recorder.onExit(() -> SwingUtilities.invokeLater(() -> window.phoneDropped(phone)));
        window.setVisible(true);
    }

    VarZinhoWindow(GymSession session, HighlightRepository repository, Feed feed) {
        super("VarZinho - Highlight Capture");
        if (session == null) throw new IllegalArgumentException("a window needs a gym session");
        if (repository == null) throw new IllegalArgumentException("a window needs a highlight repository");
        if (feed == null) throw new IllegalArgumentException("a window needs a feed of footage");
        this.gym = session.gym();
        this.feed = feed;
        this.clock = new Timer(feed.periodMillis(), event -> advanceFeed());
        this.courtPanel = new CourtPanel(gym, BUFFER_SECONDS, this::showError);
        this.archivePanel = new ArchivePanel(gym, repository, ZoneId.systemDefault(), this::showError);
        this.capturePanel = new CapturePanel(courtPanel, repository, ZoneId.systemDefault(), this::showError,
                highlight -> archivePanel.refresh());
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
        clock.start();
    }

    private JLabel buildTitle() {
        JLabel title = new JLabel(gym.getName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        return title;
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
    // recording camera and says so.
    private void phoneDropped(Camera phone) {
        if (phone.isRecording()) {
            phone.stopRecording();
        }
        showBufferStatus();
        showError("The phone stopped streaming, so its court can no longer capture. "
                + "Restart VarZinho to reconnect.");
    }

    private void advanceFeed() {
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

    private static void showStartupError(String message) {
        JOptionPane.showMessageDialog(null, message, "Saved gym not loaded", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }
}
