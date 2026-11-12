package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.PendingCapture;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * The Save highlight button: the on-screen stand-in for the physical button the
 * athlete presses on the court.
 *
 * <p>It is the only capture action in the window. Recording is continuous, so a press
 * just asks {@link CaptureHighlight} to freeze the selected court's buffer; nothing is
 * saved when the court refuses. The panel decides only which thread runs each step.
 */
final class CapturePanel extends JPanel {
    private final CourtPanel courtPanel;
    private final CaptureHighlight captureHighlight;
    private final Consumer<String> errorSink;
    private final Consumer<HighlightView> onSaved;
    private final DateTimeFormatter timeFormat;
    private final JButton save = new JButton();
    private final JLabel confirmation = new JLabel(" ");

    CapturePanel(CourtPanel courtPanel, CaptureHighlight captureHighlight, ZoneId zone,
                 Consumer<String> errorSink, Consumer<HighlightView> onSaved) {
        super(new BorderLayout(4, 4));
        if (courtPanel == null) throw new IllegalArgumentException("a capture panel needs a court panel");
        if (captureHighlight == null) throw new IllegalArgumentException("a capture panel needs a way to capture");
        if (zone == null) throw new IllegalArgumentException("a capture panel needs a time zone to show times in");
        if (errorSink == null) throw new IllegalArgumentException("a capture panel needs somewhere to report errors");
        if (onSaved == null) throw new IllegalArgumentException("a capture panel needs someone to tell about saves");
        this.courtPanel = courtPanel;
        this.captureHighlight = captureHighlight;
        this.timeFormat = ArchivePanel.timeFormatIn(zone);
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
    // capture refuses to be archived before that file exists.
    private void saveHighlight() {
        PendingCapture capture;
        try {
            capture = captureHighlight.capture(courtPanel.selectedCourtNumber());
        } catch (NoActiveCameraException | EmptyBufferException | ClipAssemblyException failure) {
            refuse(failure.getMessage());
            return;
        }
        if (!capture.hasClipToWrite()) {
            archive(capture);
            return;
        }
        HighlightView shown = capture.view();
        confirmation.setText("Saving: court " + shown.getCourtNumber() + " · camera " + shown.getCameraId() + "…");
        new ClipWrite(capture).execute();
    }

    private void archive(PendingCapture capture) {
        HighlightView archived;
        try {
            archived = capture.archive();
        } catch (RepositoryException failure) {
            refuse("Archive error: " + failure.getMessage());
            return;
        }
        confirmation.setText("Saved: court " + archived.getCourtNumber()
                + " · camera " + archived.getCameraId()
                + " · " + timeFormat.format(archived.getCapturedAt())
                + " · " + archived.getDurationSeconds() + "s");
        onSaved.accept(archived);
    }

    private void refuse(String reason) {
        confirmation.setText(" ");
        errorSink.accept(reason);
    }

    /** Writes one capture's file off the Swing thread, and archives it back on it. */
    private final class ClipWrite extends SwingWorker<Void, Void> {
        private final PendingCapture capture;

        ClipWrite(PendingCapture capture) {
            this.capture = capture;
        }

        @Override
        protected Void doInBackground() {
            capture.writeClip();
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                archive(capture);
            } catch (ExecutionException failure) {
                refuse(failure.getCause().getMessage());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                refuse("The clip was still being written when it was interrupted.");
            }
        }
    }

    private void showCourt() {
        save.setText("Save highlight - court " + courtPanel.selectedCourtNumber());
        confirmation.setText(" ");
    }
}
