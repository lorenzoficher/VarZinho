package br.edu.unipampa.varzinho.domain.highlight;

import java.time.Instant;

public final class Highlight {
    private final String id;
    private final Instant capturedAt;
    private final int courtNumber;
    private final String cameraId;
    private final VideoClip clip;

    public Highlight(String id, Instant capturedAt, int courtNumber, String cameraId, VideoClip clip) {
        if (id == null || id.isBlank() || capturedAt == null || courtNumber <= 0
                || cameraId == null || cameraId.isBlank() || clip == null) {
            throw new IllegalArgumentException("a highlight needs valid capture metadata");
        }
        this.id = id;
        this.capturedAt = capturedAt;
        this.courtNumber = courtNumber;
        this.cameraId = cameraId;
        this.clip = clip;
    }

    public String getId() { return id; }
    public Instant getCapturedAt() { return capturedAt; }
    public int getCourtNumber() { return courtNumber; }
    public String getCameraId() { return cameraId; }
    public VideoClip getClip() { return clip; }
}
