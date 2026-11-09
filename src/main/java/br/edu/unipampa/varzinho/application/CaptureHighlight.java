package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Optional;

/**
 * A press of the button: freezes a court's footage and hands back the steps that are
 * left before it is in the archive.
 *
 * <p>It only sequences. Which camera answers and what a clip holds is decided by the
 * court; which thread runs each step is decided by the caller.
 */
public final class CaptureHighlight {

    private final Gym gym;
    private final HighlightRepository repository;
    private final PendingClipWrites pendingWrites;

    public CaptureHighlight(Gym gym, HighlightRepository repository, PendingClipWrites pendingWrites) {
        if (gym == null) throw new IllegalArgumentException("capturing a highlight needs a gym");
        if (repository == null) throw new IllegalArgumentException("capturing a highlight needs a highlight repository");
        if (pendingWrites == null) throw new IllegalArgumentException("capturing a highlight needs the clips it has to write");
        this.gym = gym;
        this.repository = repository;
        this.pendingWrites = pendingWrites;
    }

    /**
     * Freezes the court's last seconds. Must run on the thread that feeds the cameras,
     * since it reads their buffers and takes from {@link PendingClipWrites}.
     *
     * @return the capture, not yet archived
     * @throws IllegalArgumentException if the gym has no court with that number
     * @throws br.edu.unipampa.varzinho.exception.NoActiveCameraException if no camera on
     *         the court is recording
     * @throws br.edu.unipampa.varzinho.exception.EmptyBufferException if the camera has
     *         recorded nothing yet
     */
    public PendingCapture capture(int courtNumber) {
        Court court = gym.findCourt(courtNumber);
        if (court == null) throw new IllegalArgumentException("court " + courtNumber + " is not registered");
        Highlight highlight = court.triggerCapture();
        Optional<Runnable> write = pendingWrites.take(highlight.getClip());
        return write.isPresent()
                ? PendingCapture.awaitingWrite(highlight, repository, write.get())
                : PendingCapture.withNothingToWrite(highlight, repository);
    }
}
