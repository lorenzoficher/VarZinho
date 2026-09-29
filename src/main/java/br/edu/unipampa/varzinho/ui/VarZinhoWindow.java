package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.time.Instant;

public final class VarZinhoWindow extends JFrame {
    private static final int BUFFER_SECONDS = 30;
    private static final int ONE_SECOND_MILLIS = 1000;

    private final Gym gym;
    private final HighlightRepository repository;
    private final LiveFeed liveFeed;
    private final Timer clock;
    private final CourtPanel courtPanel;
    private final JTextArea output = new JTextArea(12, 64);
    private final JLabel bufferStatus = new JLabel();

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            HighlightRepository repository = new CsvHighlightRepository(
                    Path.of("data", "highlights.csv"));
            new VarZinhoWindow(SampleGym.build(), repository).setVisible(true);
        });
    }

    public VarZinhoWindow(Gym gym, HighlightRepository repository) {
        super("VarZinho - Highlight Capture");
        if (gym == null) throw new IllegalArgumentException("a window needs a gym");
        if (gym.getCourts().isEmpty()) throw new IllegalArgumentException("a window needs a gym with a court");
        if (repository == null) throw new IllegalArgumentException("a window needs a highlight repository");
        this.gym = gym;
        this.repository = repository;
        this.liveFeed = new LiveFeed(gym, BUFFER_SECONDS);
        this.clock = new Timer(ONE_SECOND_MILLIS, event -> recordOneSecond());
        this.courtPanel = new CourtPanel(gym, this::showError);
        setTitle("VarZinho - " + gym.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(buildHeader(), BorderLayout.NORTH);
        top.add(courtPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        add(new JScrollPane(output), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        showBufferStatus();
        bottom.add(bufferStatus, BorderLayout.NORTH);
        bottom.add(buildActions(), BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
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

    private JPanel buildActions() {
        JButton capture = new JButton("Trigger capture");
        capture.addActionListener(event -> triggerCapture());
        JButton archive = new JButton("List archive for court");
        archive.addActionListener(event -> listArchive());
        JPanel panel = new JPanel();
        panel.add(capture); panel.add(archive);
        return panel;
    }

    private void recordOneSecond() {
        liveFeed.tick(Instant.now());
        showBufferStatus();
    }

    private void showBufferStatus() {
        Court court = selectedCourt();
        bufferStatus.setText("Court " + court.getNumber() + " buffer: "
                + liveFeed.secondsHeld(court) + " of " + BUFFER_SECONDS + " seconds.");
    }

    private void triggerCapture() {
        try {
            Highlight highlight = selectedCourt().triggerCapture();
            repository.save(highlight);
            output.append("Captured and saved: " + highlight.describe() + "\n");
        } catch (NoActiveCameraException | EmptyBufferException exception) {
            showError(exception.getMessage());
        } catch (RepositoryException exception) {
            showError("Archive error: " + exception.getMessage());
        }
    }

    private void listArchive() {
        int number = selectedCourt().getNumber();
        try {
            var highlights = repository.findByCourt(number);
            output.setText("Archive for court " + number + ":\n");
            if (highlights.isEmpty()) output.append("No highlights found.\n");
            highlights.forEach(item -> output.append(item.describe() + "\n"));
        } catch (RepositoryException exception) {
            showError("Archive error: " + exception.getMessage());
        }
    }

    private Court selectedCourt() {
        return courtPanel.selectedCourt();
    }

    private void showError(String message) {
        output.append("Error: " + message + "\n");
        JOptionPane.showMessageDialog(this, message, "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }
}
