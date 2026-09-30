package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * The Save highlight button: the on-screen stand-in for the physical button the
 * athlete presses on the court.
 *
 * <p>It is the only capture action in the window. Recording is continuous, so a press
 * just freezes the selected court's buffer and archives it; nothing is saved when the
 * court refuses.
 */
final class CapturePanel extends JPanel {
    private final CourtPanel courtPanel;
    private final HighlightRepository repository;
    private final Consumer<String> errorSink;
    private final Consumer<Highlight> onSaved;
    private final DateTimeFormatter timeFormat;
    private final JButton save = new JButton();
    private final JLabel confirmation = new JLabel(" ");

    CapturePanel(CourtPanel courtPanel, HighlightRepository repository, ZoneId zone,
                 Consumer<String> errorSink, Consumer<Highlight> onSaved) {
        super(new BorderLayout(4, 4));
        if (courtPanel == null) throw new IllegalArgumentException("a capture panel needs a court panel");
        if (repository == null) throw new IllegalArgumentException("a capture panel needs a highlight repository");
        if (zone == null) throw new IllegalArgumentException("a capture panel needs a time zone to show times in");
        this.timeFormat = ArchivePanel.timeFormatIn(zone);
        if (errorSink == null) throw new IllegalArgumentException("a capture panel needs somewhere to report errors");
        if (onSaved == null) throw new IllegalArgumentException("a capture panel needs someone to tell about saves");
        this.courtPanel = courtPanel;
        this.repository = repository;
        this.errorSink = errorSink;
        this.onSaved = onSaved;
        save.setFont(save.getFont().deriveFont(Font.BOLD, 20f));
        save.addActionListener(event -> saveHighlight());
        courtPanel.onCourtChange(this::showCourt);
        add(save, BorderLayout.CENTER);
        add(confirmation, BorderLayout.SOUTH);
        showCourt();
    }

    JButton saveButton() { return save; }

    String confirmation() {
        return confirmation.getText().trim();
    }

    private void saveHighlight() {
        try {
            Highlight highlight = courtPanel.selectedCourt().triggerCapture();
            repository.save(highlight);

            confirmation.setText("Saved: court " + highlight.getCourtNumber()
                    + " · camera " + highlight.getCameraId()
                    + " · " + timeFormat.format(highlight.getCapturedAt())
                    + " · " + highlight.getClip().getDurationSeconds() + "s");

            onSaved.accept(highlight);

        } catch (NoActiveCameraException | EmptyBufferException failure) {
            confirmation.setText(" ");
            errorSink.accept(failure.getMessage());

        } catch (RepositoryException failure) {
            confirmation.setText(" ");
            errorSink.accept("Archive error: " + failure.getMessage());
        }
    }

    private void showCourt() {
        Court court = courtPanel.selectedCourt();
        save.setText("Save highlight - court " + court.getNumber());
        confirmation.setText(" ");
    }
}
