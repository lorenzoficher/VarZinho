package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.time.Instant;

public final class VarZinhoWindow extends JFrame {
    private static final int BUFFER_SECONDS = 30;
    private static final int ONE_SECOND_MILLIS = 1000;

    private final HighlightRepository repository;
    private final JTextField gymName = new JTextField("VarZinho Arena");
    private final JTextField address = new JTextField("100 Sports Avenue");
    private final JTextField courtNumber = new JTextField("1");
    private final JTextField cameraId = new JTextField("camera-1");
    private final JCheckBox activeCamera = new JCheckBox("Start camera after installation", true);
    private final JTextArea output = new JTextArea(12, 64);
    private final JLabel bufferStatus = new JLabel("No court registered yet.");
    private final Timer clock = new Timer(ONE_SECOND_MILLIS, event -> recordOneSecond());
    private Court court;
    private LiveFeed liveFeed;

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            HighlightRepository repository = new CsvHighlightRepository(
                    Path.of("data", "highlights.csv"));
            new VarZinhoWindow(repository).setVisible(true);
        });
    }

    public VarZinhoWindow(HighlightRepository repository) {
        super("VarZinho - Highlight Capture");
        if (repository == null) throw new IllegalArgumentException("a window needs a highlight repository");
        this.repository = repository;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(buildForm(), BorderLayout.NORTH);
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        add(new JScrollPane(output), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(bufferStatus, BorderLayout.NORTH);
        bottom.add(buildActions(), BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pack();
        setLocationRelativeTo(null);
        clock.start();
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
        panel.add(new JLabel("Gym name")); panel.add(gymName);
        panel.add(new JLabel("Address")); panel.add(address);
        panel.add(new JLabel("Court number")); panel.add(courtNumber);
        panel.add(new JLabel("Camera id")); panel.add(cameraId);
        panel.add(new JLabel("Camera status")); panel.add(activeCamera);
        return panel;
    }

    private JPanel buildActions() {
        JButton register = new JButton("Register gym and install camera");
        register.addActionListener(event -> registerGym());
        JButton capture = new JButton("Trigger capture");
        capture.addActionListener(event -> triggerCapture());
        JButton archive = new JButton("List archive for court");
        archive.addActionListener(event -> listArchive());
        JPanel panel = new JPanel();
        panel.add(register); panel.add(capture); panel.add(archive);
        return panel;
    }

    private void registerGym() {
        try {
            int number = parseCourtNumber();
            Gym registeredGym = new Gym(gymName.getText(), address.getText());
            Court registeredCourt = new Court(number);
            registeredGym.addCourt(registeredCourt);
            FixedCamera camera = new FixedCamera(cameraId.getText(), "UI camera", Resolution.FULL_HD, BUFFER_SECONDS, 45);
            registeredCourt.installCamera(camera);
            if (activeCamera.isSelected()) camera.startRecording();
            court = registeredCourt;
            liveFeed = new LiveFeed(registeredGym, BUFFER_SECONDS);
            showBufferStatus();
            output.setText("Registered " + registeredGym.getName() + ", court " + number
                    + ", camera " + camera.getId() + ".\n");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void recordOneSecond() {
        if (liveFeed == null) return;
        liveFeed.tick(Instant.now());
        showBufferStatus();
    }

    private void showBufferStatus() {
        bufferStatus.setText("Court " + court.getNumber() + " buffer: "
                + liveFeed.secondsHeld(court) + " of " + BUFFER_SECONDS + " seconds.");
    }

    private void triggerCapture() {
        if (!requireCourt()) return;
        try {
            Highlight highlight = court.triggerCapture();
            repository.save(highlight);
            output.append("Captured and saved: " + highlight.describe() + "\n");
        } catch (NoActiveCameraException | EmptyBufferException exception) {
            showError(exception.getMessage());
        } catch (RepositoryException exception) {
            showError("Archive error: " + exception.getMessage());
        }
    }

    private void listArchive() {
        try {
            int number = parseCourtNumber();
            var highlights = repository.findByCourt(number);
            output.setText("Archive for court " + number + ":\n");
            if (highlights.isEmpty()) output.append("No highlights found.\n");
            highlights.forEach(item -> output.append(item.describe() + "\n"));
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (RepositoryException exception) {
            showError("Archive error: " + exception.getMessage());
        }
    }

    private int parseCourtNumber() {
        try {
            return Integer.parseInt(courtNumber.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Court number must be a whole number.");
        }
    }

    private boolean requireCourt() {
        if (court == null) {
            showError("Register a gym and court first.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        output.append("Error: " + message + "\n");
        JOptionPane.showMessageDialog(this, message, "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }
}
