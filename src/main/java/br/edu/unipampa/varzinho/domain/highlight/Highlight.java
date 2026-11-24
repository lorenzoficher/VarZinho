package br.edu.unipampa.varzinho.domain.highlight;

import java.time.Instant;

/**
 * A moment of play kept for good: what one court's camera had recorded when someone
 * pressed the button, frozen into a clip.
 *
 * <p>Immutable. It records no person, because the button carries no identity, so
 * there is no author to fill in later. Giving it a name does not change it either:
 * {@link #named(String)} returns a new highlight.
 */
public final class Highlight {
    private final String id;
    private final Instant capturedAt;
    private final int courtNumber;
    private final String cameraId;
    private final VideoClip clip;
    private final String title;

    /**
     * Records a capture that nobody has named yet.
     *
     * @param id the identifier that tells this highlight apart from every other
     * @param capturedAt the instant the button was pressed
     * @param courtNumber the court the capture came from, starting at 1
     * @param cameraId the camera whose recording became the clip
     * @param clip the saved clip file
     * @throws IllegalArgumentException if any argument is missing or blank, or the
     *         court number is not positive
     */
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

    /**
     * {@return one line a person can read, with the id, court, camera, capture
     * instant, clip length and file path}
     */
    public String describe() {
        return id + " | court " + courtNumber + " | camera " + cameraId + " | "
                + capturedAt + " | " + clip.getDurationSeconds() + "s | " + clip.getFilePath();
    }

    /** {@return the identifier, never blank} */
    public String getId() { return id; }

    /** {@return the name someone gave this highlight, or an empty string if nobody has} */
    public String getTitle() { return title; }

    /**
     * Checks a name before it goes on a highlight. Public so that a repository can
     * refuse a bad name before it writes anything.
     *
     * @param title the name as someone typed it
     * @return the name without leading or trailing spaces
     * @throws IllegalArgumentException if the name is missing, blank, or contains a
     *         line break or another control character
     */
    public static String requireTitle(String title) {
        if (title == null || title.isBlank() || title.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("a recording needs a name on one line");
        }
        return title.strip();
    }

    /**
     * Gives the highlight a name. This highlight stays as it was.
     *
     * @param title the name, checked by {@link #requireTitle(String)}
     * @return a new highlight with the same capture and the name applied
     * @throws IllegalArgumentException if {@link #requireTitle(String)} rejects the name
     */
    public Highlight named(String title) {
        return new Highlight(id, capturedAt, courtNumber, cameraId, clip, requireTitle(title));
    }

    /** {@return the instant the button was pressed} */
    public Instant getCapturedAt() { return capturedAt; }

    /** {@return the number of the court the capture came from, always positive} */
    public int getCourtNumber() { return courtNumber; }

    /** {@return the identifier of the camera whose recording became the clip} */
    public String getCameraId() { return cameraId; }

    /** {@return the record of the saved clip file} */
    public VideoClip getClip() { return clip; }
}
