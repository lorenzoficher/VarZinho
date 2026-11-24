package br.edu.unipampa.varzinho.domain.capture;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/**
 * A camera bolted to the structure, aimed once when it is installed and never moved.
 *
 * <p>Its angle is part of what it is, not a setting: pointing it somewhere else means
 * climbing up and remounting it.
 */
public final class FixedCamera extends Camera {

    private final int angle;

    /**
     * Builds a fixed camera, switched off, aimed at the angle it was mounted at.
     *
     * @param id the identifier, unique across the gym the camera is installed in
     * @param model the make and model of the equipment
     * @param resolution the resolution it records at
     * @param bufferSeconds how many of the most recent seconds the camera keeps
     * @param angle where it was aimed when mounted, in degrees (0–359 inclusive)
     * @throws IllegalArgumentException if the id or model is missing or blank, the
     *         resolution is missing, the buffer is not at least one second long, or
     *         the angle is outside the 0–359 range
     */
    public FixedCamera(String id, String model, Resolution resolution, int bufferSeconds, int angle) {
        super(id, model, resolution, bufferSeconds);
        if (angle < 0 || angle > 359) {
            throw new IllegalArgumentException(
                    "angle must be between 0 and 359 (inclusive), but was " + angle);
        }
        this.angle = angle;
    }

    @Override
    public VideoClip captureLastSeconds(int seconds) {
        List<Frame> window = recordedWindow(seconds);
        return clipFrom(clipPath(window), seconds);
    }

    @Override
    public String describe() {
        return describeAs("fixed");
    }

    private String clipPath(List<Frame> window) {
        Frame last = window.get(window.size() - 1);
        return clipName(last) + ".mp4";
    }

    /** {@return where the camera was aimed when mounted, in degrees; it never changes} */
    public int getAngle() {
        return angle;
    }
}
