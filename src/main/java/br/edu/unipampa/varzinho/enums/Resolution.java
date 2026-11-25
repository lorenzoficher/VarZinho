package br.edu.unipampa.varzinho.enums;

/**
 * Picture size a camera records at, and that its clips are stored as.
 *
 * <p>No frame is ever decoded in this project, so the dimensions are metadata: they
 * describe the clip, they do not constrain it.
 */
public enum Resolution {

    /** 720p — 1280×720 pixels. */
    HD("HD", 1280, 720),
    /** 1080p — 1920×1080 pixels. */
    FULL_HD("Full HD", 1920, 1080),
    /** 4K — 3840×2160 pixels. */
    ULTRA_HD("Ultra HD", 3840, 2160);

    private final String displayName;
    private final int width;
    private final int height;

    Resolution(String displayName, int width, int height) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
    }

    /** {@return the horizontal pixel count for this resolution} */
    public int width() {
        return width;
    }

    /** {@return the vertical pixel count for this resolution} */
    public int height() {
        return height;
    }

    /**
     * How this resolution is written for a person to read.
     *
     * <p>For persistence use {@link #name()} instead — the archive round-trips
     * through {@link #valueOf(String)}, which knows nothing about this text.
     *
     * @return the display name followed by the dimensions, as {@code Full HD (1920x1080)}
     */
    public String label() {
        return displayName + " (" + width + "x" + height + ")";
    }
}
