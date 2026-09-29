package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Frame;
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
import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

public final class VarZinhoWindow extends JFrame {
    private final Gym gym;
    private final HighlightRepository repository;
    private final JTextArea output = new JTextArea(12, 64);
    private int frameSequence;
    private Instant lastFrameAt;

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
        setTitle("VarZinho - " + gym.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(buildHeader(), BorderLayout.NORTH);
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pack();
        setLocationRelativeTo(null);
    }

    private JLabel buildHeader() {
        JLabel header = new JLabel(gym.getName());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        return header;
    }

    private JPanel buildActions() {
        JButton record = new JButton("Record " + Court.DEFAULT_CAPTURE_SECONDS + " seconds");
        record.addActionListener(event -> recordFrames());
        JButton capture = new JButton("Trigger capture");
        capture.addActionListener(event -> triggerCapture());
        JButton archive = new JButton("List archive for court");
        archive.addActionListener(event -> listArchive());
        JPanel panel = new JPanel();
        panel.add(record); panel.add(capture); panel.add(archive);
        return panel;
    }

    private void recordFrames() {
        Court court = selectedCourt();
        if (!court.hasActiveCamera()) {
            showError("Court " + court.getNumber() + " has no active camera.");
            return;
        }
        Instant firstFrameAt = Instant.now().minusSeconds(Court.DEFAULT_CAPTURE_SECONDS - 1L);
        if (lastFrameAt != null && !firstFrameAt.isAfter(lastFrameAt)) {
            firstFrameAt = lastFrameAt.plusNanos(1);
        }
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            lastFrameAt = firstFrameAt.plusNanos(second);
            court.record(new Frame(lastFrameAt, frameSequence++));
        }
        output.append("Recorded " + Court.DEFAULT_CAPTURE_SECONDS + " seconds on court "
                + court.getNumber() + ".\n");
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

    // Until the window lets the viewer pick a court, every action targets the lowest-numbered one.
    private Court selectedCourt() {
        return gym.getCourts().stream()
                .min(Comparator.comparingInt(Court::getNumber))
                .orElseThrow(() -> new IllegalStateException("the gym has no court"));
    }

    private void showError(String message) {
        output.append("Error: " + message + "\n");
        JOptionPane.showMessageDialog(this, message, "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }
}
