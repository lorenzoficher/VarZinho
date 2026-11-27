package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;

import java.time.Instant;

/**
 * The immutable highlight data an interface may display without receiving a domain entity.
 */
public final class HighlightView {

    private final Instant capturedAt;
    private final String id;
    private final String title;
    private final int courtNumber;
    private final String cameraId;
    private final int durationSeconds;
    private final String clipPath;

    private HighlightView(String id, Instant capturedAt, int courtNumber, String cameraId,
            int durationSeconds, String clipPath, String title) {
        this.capturedAt = capturedAt;
        this.id = id;
        this.title = title;
        this.courtNumber = courtNumber;
        this.cameraId = cameraId;
        this.durationSeconds = durationSeconds;
        this.clipPath = clipPath;
    }

    static HighlightView from(Highlight highlight) {
        return new HighlightView(highlight.getId(), highlight.getCapturedAt(), highlight.getCourtNumber(),
                highlight.getCameraId(), highlight.getClip().getDurationSeconds(),
                highlight.getClip().getFilePath(), highlight.getTitle());
    }

    /** {@return the instant the button was pressed} */
    public Instant getCapturedAt() {
        return capturedAt;
    }

    /** {@return the highlight's identifier} */
    public String getId() { return id; }

    /** {@return the name someone gave the highlight, or an empty string if nobody has} */
    public String getTitle() { return title; }

    /** {@return the number of the court the capture came from} */
    public int getCourtNumber() {
        return courtNumber;
    }

    /** {@return the identifier of the camera whose recording became the clip} */
    public String getCameraId() {
        return cameraId;
    }

    /** {@return how many seconds the clip lasts} */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /** {@return where the clip is, as stored with the highlight} */
    public String getClipPath() {
        return clipPath;
    }
}
