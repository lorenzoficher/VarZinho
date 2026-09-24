package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.exception.RepositoryException;
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
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.Instant;

public final class VarZinhoWindow extends JFrame {
    private final HighlightRepository repository;
    private final JTextField gymName = new JTextField("VarZinho Arena");
    private final JTextField address = new JTextField("100 Sports Avenue");
    private final JTextField courtNumber = new JTextField("1");
    private final JTextField cameraId = new JTextField("camera-1");
    private final JCheckBox activeCamera = new JCheckBox("Start camera after installation", true);
    private final JTextArea output = new JTextArea(12, 64);
    private Gym gym;
    private Court court;
    private int frameSequence;

    public VarZinhoWindow(HighlightRepository repository) {
        super("VarZinho — Highlight Capture");
        this.repository = repository;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(buildForm(), BorderLayout.NORTH);
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pack();
        setLocationRelativeTo(null);
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
        JButton record = new JButton("Record 5 seconds");
        record.addActionListener(event -> recordFrames());
        JButton capture = new JButton("Trigger capture");
        capture.addActionListener(event -> triggerCapture());
        JButton archive = new JButton("List archive for court");
        archive.addActionListener(event -> listArchive());
        JPanel panel = new JPanel();
        panel.add(register); panel.add(record); panel.add(capture); panel.add(archive);
        return panel;
    }

    private void registerGym() {
        try {
            int number = parseCourtNumber();
            gym = new Gym(gymName.getText(), address.getText());
            court = new Court(number);
            gym.addCourt(court);
            FixedCamera camera = new FixedCamera(cameraId.getText(), "UI camera", Resolution.FULL_HD, 30, 45);
            court.installCamera(camera);
            if (activeCamera.isSelected()) camera.startRecording();
            output.setText("Registered " + gym.getName() + ", court " + number
                    + ", camera " + camera.getId() + ".\n");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void recordFrames() {
        if (!requireCourt()) return;
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            court.record(new Frame(Instant.now().plusSeconds(second), frameSequence++));
        }
        output.append("Recorded 5 seconds on court " + court.getNumber() + ".\n");
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
            output.setText("Archive for court " + number + ":\n");
            var highlights = repository.findByCourt(number);
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
