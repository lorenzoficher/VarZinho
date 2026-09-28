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

    public FixedCamera(String id, String model, Resolution resolution, int bufferSeconds, int angle) {
        super(id, model, resolution, bufferSeconds);
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
        return "clips/" + getId() + "-" + last.getTimestamp().getEpochSecond() + ".mp4";
    }

    public int getAngle() {
        return angle;
    }
}
