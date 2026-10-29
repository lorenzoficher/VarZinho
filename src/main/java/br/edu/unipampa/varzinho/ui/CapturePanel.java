package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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
    private final PendingClipWrites pendingWrites;
    private final Consumer<String> errorSink;
    private final Consumer<Highlight> onSaved;
    private final DateTimeFormatter timeFormat;
    private final JButton save = new JButton();
    private final JLabel confirmation = new JLabel(" ");

    CapturePanel(CourtPanel courtPanel, HighlightRepository repository, ZoneId zone, PendingClipWrites pendingWrites,
                 Consumer<String> errorSink, Consumer<Highlight> onSaved) {
        super(new BorderLayout(4, 4));
        if (courtPanel == null) throw new IllegalArgumentException("a capture panel needs a court panel");
        if (repository == null) throw new IllegalArgumentException("a capture panel needs a highlight repository");
        if (zone == null) throw new IllegalArgumentException("a capture panel needs a time zone to show times in");
        this.timeFormat = ArchivePanel.timeFormatIn(zone);
        if (pendingWrites == null) throw new IllegalArgumentException("a capture panel needs the clips it has to write");
        if (errorSink == null) throw new IllegalArgumentException("a capture panel needs somewhere to report errors");
        if (onSaved == null) throw new IllegalArgumentException("a capture panel needs someone to tell about saves");
        this.courtPanel = courtPanel;
        this.repository = repository;
        this.pendingWrites = pendingWrites;
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

    // A phone's file is written on a worker so the window and the feeds keep going; the
    // highlight is archived only once that file exists, never before.
    private void saveHighlight() {
        Highlight highlight;
        try {
            highlight = courtPanel.selectedCourt().triggerCapture();
        } catch (NoActiveCameraException | EmptyBufferException | ClipAssemblyException failure) {
            refuse(failure.getMessage());
            return;
        }
        Optional<Runnable> write = pendingWrites.take(highlight.getClip());
        if (write.isEmpty()) {
            archive(highlight);
            return;
        }
        confirmation.setText("Saving: court " + highlight.getCourtNumber()
                + " · camera " + highlight.getCameraId() + "…");
        new ClipWrite(write.get(), highlight).execute();
    }

    private void archive(Highlight highlight) {
        try {
            repository.save(highlight);
        } catch (RepositoryException failure) {
            refuse("Archive error: " + failure.getMessage());
            return;
        }
        confirmation.setText("Saved: court " + highlight.getCourtNumber()
                + " · camera " + highlight.getCameraId()
                + " · " + timeFormat.format(highlight.getCapturedAt())
                + " · " + highlight.getClip().getDurationSeconds() + "s");
        onSaved.accept(highlight);
    }

    private void refuse(String reason) {
        confirmation.setText(" ");
        errorSink.accept(reason);
    }

    /** Writes one highlight's file off the Swing thread, and archives it back on it. */
    private final class ClipWrite extends SwingWorker<Void, Void> {
        private final Runnable write;
        private final Highlight highlight;

        ClipWrite(Runnable write, Highlight highlight) {
            this.write = write;
            this.highlight = highlight;
        }

        @Override
        protected Void doInBackground() {
            write.run();
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                archive(highlight);
            } catch (ExecutionException failure) {
                refuse(failure.getCause().getMessage());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                refuse("The clip was still being written when it was interrupted.");
            }
        }
    }

    private void showCourt() {
        Court court = courtPanel.selectedCourt();
        save.setText("Save highlight - court " + court.getNumber());
        confirmation.setText(" ");
    }
}
