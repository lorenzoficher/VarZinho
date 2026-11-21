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

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }

    public int getCourtNumber() {
        return courtNumber;
    }

    public String getCameraId() {
        return cameraId;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getClipPath() {
        return clipPath;
    }
}
