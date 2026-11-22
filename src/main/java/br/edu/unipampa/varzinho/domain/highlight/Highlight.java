package br.edu.unipampa.varzinho.domain.highlight;

import java.time.Instant;

public final class Highlight {
    private final String id;
    private final Instant capturedAt;
    private final int courtNumber;
    private final String cameraId;
    private final VideoClip clip;
    private final String title;

    public Highlight(String id, Instant capturedAt, int courtNumber, String cameraId, VideoClip clip) {
        this(id, capturedAt, courtNumber, cameraId, clip, "");
    }

    private Highlight(String id, Instant capturedAt, int courtNumber, String cameraId, VideoClip clip, String title) {
        if (id == null || id.isBlank() || capturedAt == null || courtNumber <= 0
                || cameraId == null || cameraId.isBlank() || clip == null) {
            throw new IllegalArgumentException("a highlight needs valid capture metadata");
        }
        this.id = id;
        this.capturedAt = capturedAt;
        this.courtNumber = courtNumber;
        this.cameraId = cameraId;
        this.clip = clip;
        this.title = title;
    }

    public String describe() {
        return id + " | court " + courtNumber + " | camera " + cameraId + " | "
                + capturedAt + " | " + clip.getDurationSeconds() + "s | " + clip.getFilePath();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }

    public static String requireTitle(String title) {
        if (title == null || title.isBlank() || title.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("a recording needs a name on one line");
        }
        return title.strip();
    }

    public Highlight named(String title) {
        return new Highlight(id, capturedAt, courtNumber, cameraId, clip, requireTitle(title));
    }
    public Instant getCapturedAt() { return capturedAt; }
    public int getCourtNumber() { return courtNumber; }
    public String getCameraId() { return cameraId; }
    public VideoClip getClip() { return clip; }
}
