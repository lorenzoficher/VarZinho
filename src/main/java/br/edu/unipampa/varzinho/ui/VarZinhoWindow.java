package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.repository.CsvGymRepository;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;

public final class VarZinhoWindow extends JFrame {
    private static final int BUFFER_SECONDS = 30;
    private static final int ONE_SECOND_MILLIS = 1000;

    private final Gym gym;
    private final LiveFeed liveFeed;
    private final Timer clock;
    private final CourtPanel courtPanel;
    private final ArchivePanel archivePanel;
    private final CapturePanel capturePanel;
    private final JLabel bufferStatus = new JLabel();

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            HighlightRepository repository = new CsvHighlightRepository(
                    Path.of("data", "highlights.csv"));
            GymSession session = GymSession.start(new CsvGymRepository(Path.of("data", "gym.csv")),
                    VarZinhoWindow::showStartupError);
            new VarZinhoWindow(session, repository).setVisible(true);
        });
    }

    VarZinhoWindow(GymSession session, HighlightRepository repository) {
        super("VarZinho - Highlight Capture");
        if (session == null) throw new IllegalArgumentException("a window needs a gym session");
        if (repository == null) throw new IllegalArgumentException("a window needs a highlight repository");
        this.gym = session.gym();
        this.liveFeed = new LiveFeed(gym, BUFFER_SECONDS);
        this.clock = new Timer(ONE_SECOND_MILLIS, event -> recordOneSecond());
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
        top.add(buildHeader(), BorderLayout.NORTH);
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

    private JLabel buildHeader() {
        JLabel header = new JLabel(gym.getName());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        return header;
    }

    private void recordOneSecond() {
        liveFeed.tick(Instant.now());
        showBufferStatus();
    }

    private void showBufferStatus() {
        Court court = courtPanel.selectedCourt();
        bufferStatus.setText("Court " + court.getNumber() + " buffer: "
                + liveFeed.secondsHeld(court) + " of " + BUFFER_SECONDS + " seconds.");
    }

    private static void showStartupError(String message) {
        JOptionPane.showMessageDialog(null, message, "Saved gym not loaded", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }
}
