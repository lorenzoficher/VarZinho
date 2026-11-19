package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

/**
 * A press of the button: captures a court's footage, including its file when the
 * camera writes one, and hands back the capture to archive.
 *
 * <p>It only sequences. Which camera answers and what a clip holds is decided by the
 * court; which thread runs each step is decided by the caller.
 */
public final class CaptureHighlight {

    private final Gym gym;
    private final HighlightRepository repository;

    public CaptureHighlight(Gym gym, HighlightRepository repository) {
        if (gym == null) throw new IllegalArgumentException("capturing a highlight needs a gym");
        if (repository == null) throw new IllegalArgumentException("capturing a highlight needs a highlight repository");
        this.gym = gym;
        this.repository = repository;
    }

    /**
     * Captures the court's last seconds. The caller must prevent concurrent feeding
     * or changes to the cameras until this returns. A stream capture waits for its
     * assembler, so the window runs the whole operation off the Swing thread.
     *
     * @return the capture with its clip finished, not yet archived
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
        return new PendingCapture(highlight, repository);
    }
}
